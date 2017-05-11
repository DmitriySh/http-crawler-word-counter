package ru.shishmakov.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.RecursiveTask;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class CrawlerCounter extends RecursiveTask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String url;
    private final int depth;

    public CrawlerCounter(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }

    @Override
    protected Void compute() {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(3_000)
                    .get();
        } catch (IOException e) {
            logger.error("Request error", e);
        }
        return null;
    }
}
