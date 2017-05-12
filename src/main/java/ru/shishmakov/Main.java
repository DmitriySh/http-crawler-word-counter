package ru.shishmakov;

import com.google.common.util.concurrent.MoreExecutors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.shishmakov.config.Config;
import ru.shishmakov.core.CrawlerCounter;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
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
            startCrawler(args);
        } catch (Exception e) {
            logger.error("Error in http-crawler process", e);
        }
        logger.info("End http-crawler process");
    }

    private static void startCrawler(String[] args) throws MalformedURLException {
        String uri = StringUtils.trimToEmpty(args[0]);
        int depth = Integer.valueOf(args[1]);
        URL url = new URL(uri);
        String baseUri = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();
        logger.debug("Incoming parameters uri: {}, depth: {}", uri, depth);

        ForkJoinPool pool = new ForkJoinPool();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(Config.class);
            context.refresh();

            logger.debug("Invoke crawler task ...");
            final CrawlerCounter task = context.getBean(CrawlerCounter.class);
            task.setUri(uri);
            task.setBaseUri(baseUri);
            task.setDepth(depth);
            pool.invoke(task);
        } finally {
            MoreExecutors.shutdownAndAwaitTermination(pool, 30, SECONDS);
        }
    }
}
