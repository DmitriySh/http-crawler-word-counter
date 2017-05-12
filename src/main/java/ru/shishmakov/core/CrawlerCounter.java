package ru.shishmakov.core;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.shishmakov.util.CrawlerUtil.getLinks;

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
    private Set<String> visitedUrls;
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
//            doJob();
        } catch (Exception e) {
            logger.error("Request error", e);
        }
        logger.info("{}: {} end", NAME, number);
    }

    private void doJob() throws IOException {
        URL url = new URL(uri);
        String baseUri = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();
        Document doc = Jsoup.parse(url, 3_000);
        doc.setBaseUri(baseUri);
        String[] texts = doc.body().text().split(StringUtils.SPACE);

        if (depth - 1 > 0) {
            List<String> links = getLinks(doc);
            tryScanNextLinks(links);
        }
    }

    private void tryScanNextLinks(List<String> links) {
        CrawlerCounter nextCrawler = forkTask();
        nextCrawler.setBaseUri(baseUri);
        nextCrawler.setUri(uri/*other uri*/);
        nextCrawler.setDepth(depth - 1);
        logger.debug("Fork next {}: {}", NAME, nextCrawler.number);
        invokeAll(nextCrawler);
    }
}
