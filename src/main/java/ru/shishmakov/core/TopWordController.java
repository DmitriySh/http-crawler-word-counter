package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class TopWordController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private CrawlerCounter crawlerCounter;
    @Inject
    private ConcurrentMap<String, Long> wordCounter;

    public void startCrawler(String uri, String baseUri, int depth) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            logger.debug("Invoke crawler task ...");
            crawlerCounter.setUri(uri);
            crawlerCounter.setBaseUri(baseUri);
            crawlerCounter.setDepth(depth);

            pool.invoke(crawlerCounter);
            printTopWords(wordCounter);
            logger.debug("THE END wordCounter: {}", wordCounter);
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(pool, 30, SECONDS);
        }

    }

    private void printTopWords(ConcurrentMap<String, Long> wordCounter) {

    }
}
