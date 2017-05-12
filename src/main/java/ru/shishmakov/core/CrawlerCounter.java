package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Parse content and fork tasks for next URLs
 *
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public abstract class CrawlerCounter extends RecursiveAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicInteger generator = new AtomicInteger(1);
    private static final String NAME = CrawlerCounter.class.getSimpleName();

    @Inject
    private Set<String> visitedUrl;
    @Inject
    private BlockingQueue<Word> wordCounter;
    @Inject
    private RateAccessController accessController;

    private final int number = generator.getAndIncrement();
    private String url;
    private int depth;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    protected void compute() {
        logger.info("{}: {} starting task [url: {}, depth: {}] ...", NAME, number, url, depth);
        checkNotNull(visitedUrl);
        checkNotNull(wordCounter);
        try {
//            Document doc = Jsoup.connect(url)
//                    .timeout(3_000)
//                    .get();
            tryScanNextLinks();
        } catch (Exception e) {
            logger.error("Request error", e);
        }
        logger.info("{}: {} end", NAME, number);
    }

    private void tryScanNextLinks() {
        if (depth - 1 > 0) {
            CrawlerCounter nextCrawler = forkTask();
            nextCrawler.setUrl(url/*other url*/);
            nextCrawler.setDepth(depth - 1);
            logger.debug("Fork next {}: {}", NAME, nextCrawler.number);
            invokeAll(nextCrawler);
        }
    }

    /**
     * Create next task for async processing new URL
     *
     * @return prototype object {@link CrawlerCounter}
     */
    public abstract CrawlerCounter forkTask();
}
