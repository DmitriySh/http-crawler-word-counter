package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class RatingController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private CrawlerCounter crawlerCounter;
    @Inject
    private ConcurrentMap<String, Long> wordCounter;

    private static final int topRating = 100;

    public void startCrawler(String uri, String baseUri, int depth) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            logger.debug("Invoke crawler task ...");
            crawlerCounter.setUri(uri);
            crawlerCounter.setBaseUri(baseUri);
            crawlerCounter.setDepth(depth);

            pool.invoke(crawlerCounter);
            printTopWords(wordCounter);
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(pool, 30, SECONDS);
        }

    }

    private void printTopWords(ConcurrentMap<String, Long> wordCounter) {
        final TreeSet<Word> top = new TreeSet<>();
        wordCounter.forEach((k, v) -> {
            if (topRating > top.size()) {
                top.add(new Word(k, v));
            } else {
                final Word word = new Word(k, v);
                if (word.compareTo(top.last()) < 0) {
                    top.pollLast();
                    top.add(word);
                }
            }
        });

        logger.info("TOP {}, size: {}\n{}", topRating, top.size(), top);
    }
}
