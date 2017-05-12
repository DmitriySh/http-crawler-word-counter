package ru.shishmakov;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.shishmakov.config.Config;
import ru.shishmakov.core.CrawlerCounter;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ForkJoinPool;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Dmitriy Shishmakov on 12.05.17
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        logger.info("Start http-crawler process");
        try {
            startCrawlerTask(args);
        } catch (Exception e) {
            logger.error("Error in time executing task", e);
        }
        logger.info("End http-crawler process");
    }

    private static void startCrawlerTask(String[] args) {
        String url = StringUtils.trimToEmpty(args[0]);
        int depth = Integer.valueOf(args[1]);
        logger.debug("Incoming parameters url: {}, depth: {}", url, depth);

        ForkJoinPool pool = new ForkJoinPool();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(Config.class);
            context.refresh();

            logger.debug("Invoke crawler task ...");
            final CrawlerCounter task = context.getBean(CrawlerCounter.class);
            task.setUrl(url);
            task.setDepth(depth);
            pool.invoke(task);
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(pool, 30, SECONDS);
        }
    }
}
