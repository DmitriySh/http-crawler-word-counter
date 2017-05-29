package ru.shishmakov.core;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.time.StopWatch;
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
import java.util.Optional;
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
            parseUri();
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

    private void parseUri() throws Exception {
        Document doc = accessController.acquireAccess(buildRequestTask());
        countElementWords(doc.body());
        if (visitedUri.isEmpty()) crawlerUtil.simplifyUri(uri).ifPresent(visitedUri::add); // root of requests

        if (depth - 1 > 0) {
            Stream<String> links = crawlerUtil.getStreamHrefLinks(doc)
                    .filter(buildPredicateByBaseUri())
                    .filter(buildPredicateByVisitedUri());
            tryScanNextLinks(links);
        }
    }

    private void countElementWords(Element element) {
        if (Objects.isNull(element)) {
            logger.warn("{}: {} skips uri: {}; site has no body element", NAME, number, uri);
            return;
        }
        List<String> textList = crawlerUtil.getText(element);
        textList.forEach(t -> wordCounter.merge(lowerCase(t), 1L, (old, inc) -> old + inc));
    }

    private void tryScanNextLinks(Stream<String> links) {
        final List<CrawlerCounter> nextCrawlers = links
                .peek(uri -> crawlerUtil.simplifyUri(uri).ifPresent(visitedUri::add))
                .map(uri -> {
                    try {
                        URI obj = new URI(uri);
                        CrawlerCounter nextCrawler = forkTask();
                        nextCrawler.setUri(obj.normalize().toString());
                        nextCrawler.setBaseUri(crawlerUtil.getBaseUri(obj));
                        nextCrawler.setDepth(depth - 1);
                        return nextCrawler;
                    } catch (URISyntaxException e) {
                        logger.error("{}: {} error define new task with uri: {}", NAME, number, uri);
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

    private Predicate<String> buildPredicateByVisitedUri() {
        return uri -> {
            final Optional<String> value = crawlerUtil.simplifyUri(uri);
            if (value.isPresent() && !visitedUri.contains(value.get())) {
                return true;
            } else if (value.isPresent() && visitedUri.contains(value.get())) {
                return false;
            } else return false;
        };
    }

    private Predicate<String> buildPredicateByBaseUri() {
        return uri -> {
            try {
                return equalsIgnoreCase(new URL(baseUri).getHost(), new URL(uri).getHost());
            } catch (MalformedURLException e) {
                logger.error("Error on define host of uri", e);
                return false;
            }
        };
    }
}
