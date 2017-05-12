package ru.shishmakov.core;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.util.CrawlerUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.shishmakov.util.CrawlerUtil.getStreamHrefLinks;
import static ru.shishmakov.util.CrawlerUtil.simplifyUri;

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
        logger.info("{}: {} starting task [uri: {}, depth: {}] ...", NAME, number, uri, depth);
        try {
            parseUri();
        } catch (Exception e) {
            logger.error("Request error", e);
        }
        logger.info("{}: {} end", NAME, number);
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

    private void parseUri() throws IOException {
        URL url = new URL(uri);
        Document doc = Jsoup.parse(url, 3_000);
        doc.setBaseUri(baseUri);

        countElementWords(doc.body());
        simplifyUri(uri).ifPresent(visitedUri::add);

        if (depth - 1 > 0) {
            Stream<String> links = getStreamHrefLinks(doc)
                    .filter(buildPredicateByBaseUri())
                    .filter(buildPredicateByVisitedUri());
            tryScanNextLinks(links);
        }
    }

    private void countElementWords(Element body) {
        List<String> textList = CrawlerUtil.getText(body);
        textList.forEach(t -> wordCounter.merge(t, 1L, (old, inc) -> old + inc));
    }

    private void tryScanNextLinks(Stream<String> links) {
        final List<CrawlerCounter> nextCrawlers = links.map(uri -> {
            CrawlerCounter nextCrawler = forkTask();
            nextCrawler.setBaseUri(baseUri);
            nextCrawler.setUri(uri);
            nextCrawler.setDepth(depth - 1);
            return nextCrawler;
        }).collect(Collectors.toList());
        logger.debug("Fork next crawlers: {} ", nextCrawlers);
        invokeAll(nextCrawlers);
    }


    private Predicate<String> buildPredicateByVisitedUri() {
        return uri -> {
            final Optional<String> value = simplifyUri(uri);
            return value.isPresent() && !visitedUri.contains(value.get());
        };
    }

    private Predicate<String> buildPredicateByBaseUri() {
        return uri -> {
            try {
                return StringUtils.equalsIgnoreCase(new URL(baseUri).getHost(), new URL(uri).getHost());
            } catch (MalformedURLException e) {
                logger.error("Error on define host of uri", e);
                return false;
            }
        };
    }
}
