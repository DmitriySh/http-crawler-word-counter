package ru.shishmakov;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.shishmakov.config.AppConfig;
import ru.shishmakov.core.RatingController;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author Dmitriy Shishmakov on 12.05.17
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        logger.info("Start http-crawler process");
        try {
            process(args);
        } catch (Exception e) {
            logger.error("Error in http-crawler process", e);
        }
        logger.info("End http-crawler process");
    }

    private static void process(String[] args) throws MalformedURLException, URISyntaxException {
        String uri = StringUtils.trimToEmpty(args[0]);
        int depth = Integer.valueOf(args[1]);
        logger.debug("Incoming parameters uri: {}, depth: {}", uri, depth);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(AppConfig.class);
            context.refresh();
            context.getBean(RatingController.class).startCrawler(uri, depth);
        }
    }
}
