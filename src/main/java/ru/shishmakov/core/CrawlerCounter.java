package ru.shishmakov.core;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.CrawlerConfig;
import ru.shishmakov.util.CrawlerUtil;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.lowerCase;

/**
 * Parse content and fork tasks for next URLs
 *
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public abstract class CrawlerCounter extends RecursiveAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicInteger generator = new AtomicInteger(1);
    private static final String NAME = CrawlerCounter.class.getSimpleName();
    private final int number = generator.getAndIncrement();
    @Inject
    private Set<String> visitedUri;
    @Inject
    private ConcurrentMap<String, Long> wordCounter;
    @Inject
    private RateAccessController accessController;
    @Inject
    private CrawlerUtil crawlerUtil;
    @Inject
    private CrawlerConfig crawlerConfig;


    private String baseUri;
    private String uri;
    private int depth;

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Create next task for async processing new URL
     *
     * @return prototype object {@link CrawlerCounter}
     */
    public abstract CrawlerCounter forkTask();

    /**
     * <pre>
     * while(list of unvisited URLs is not empty) {
     * take URL from list
     * fetch content
     * record whatever it is you want to about the content
     *    if content is HTML {
     *        parse out URLs from links
     *           foreach URL {
     *               if it matches your rules
     *               and it's not already in either the visited or unvisited list
     *                   add it to the unvisited list
     *        }
     *    }
     * }
     * </pre>
     */
    @Override
    protected void compute() {
        final StopWatch watch = StopWatch.createStarted();
        logger.info("{}: {} starting task [uri: {}, depth: {}] ...", NAME, number, uri, depth);
        try {
            parseLink();
        } catch (Exception e) {
            logger.error("{}: {} error request on uri: {}", NAME, number, uri, e);
        }
        watch.stop();
        logger.info("{}: {} end; elapsed: {} ms", NAME, number, watch.getTime());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("number", number)
                .add("baseUri", baseUri)
                .add("uri", uri)
                .add("depth", depth)
                .toString();
    }

    private void parseLink() throws Exception {
        Callable<Document> task = buildRequestTask();
        Document doc = accessController.acquireAccess(task);
        countElementWords(doc.body());
        if (visitedUri.isEmpty()) visitedUri.add(crawlerUtil.simplifyUri(uri)); // root of requests

        if (depth - 1 > 0) {
            tryParseNextLinks(crawlerUtil.getStreamHrefLinks(doc));
        }
    }

    private void countElementWords(Element element) {
        if (Objects.isNull(element)) {
            logger.warn("{}: {} skips uri: {}; site has no element", NAME, number, uri);
            return;
        }
        List<String> textList = crawlerUtil.getText(element);
        textList.forEach(t -> wordCounter.merge(lowerCase(t), 1L, (old, inc) -> old + inc));
    }

    private void tryParseNextLinks(Stream<String> links) {
        final List<CrawlerCounter> nextCrawlers = links
                .map(uri -> Pair.of(uri, crawlerUtil.simplifyUri(uri)))
                .filter(isLegalBaseHost())
                .filter(isNotEmail())
                .filter(isNotVisitedUri())
                .map(p -> {
                    try {
                        URI obj = new URI(p.getKey());
                        CrawlerCounter nextCrawler = forkTask();
                        nextCrawler.setUri(obj.normalize().toString());
                        nextCrawler.setBaseUri(crawlerUtil.getBaseUri(obj));
                        nextCrawler.setDepth(depth - 1);
                        return nextCrawler;
                    } catch (URISyntaxException e) {
                        logger.error("{}: {} error define new task with uri: {}", NAME, number, p.getKey());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        invokeAll(nextCrawlers);
    }

    private Callable<Document> buildRequestTask() {
        return () -> {
            URL url = new URL(uri);
            Document doc = Jsoup.parse(url, crawlerConfig.requestTimeoutMs());
            doc.setBaseUri(baseUri);
            return doc;
        };
    }

    private Predicate<Pair<String, String>> isNotVisitedUri() {
        return p -> {
            if (visitedUri.contains(p.getValue())) return false;
            else {
                visitedUri.add(p.getValue());
                return true;
            }
        };
    }

    private Predicate<Pair<String, String>> isNotEmail() {
        return p -> !StringUtils.contains(p.getKey(), '@');
    }

    private Predicate<Pair<String, String>> isLegalBaseHost() {
        return p -> {
            try {
                return equalsIgnoreCase(new URL(baseUri).getHost(), new URL(p.getKey()).getHost());
            } catch (MalformedURLException e) {
                logger.error("Error on define host of uri", e);
                return false;
            }
        };
    }
}
