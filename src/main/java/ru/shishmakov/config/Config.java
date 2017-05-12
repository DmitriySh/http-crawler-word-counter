package ru.shishmakov.config;

import com.google.common.collect.Sets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.shishmakov.core.CrawlerCounter;
import ru.shishmakov.core.RateAccessController;
import ru.shishmakov.core.Word;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

/**
 * @author Dmitriy Shishmakov on 12.05.17
 */
@Configuration
public class Config {

    @Bean(name = "wordCounter")
    public BlockingQueue<Word> wordCounter() {
        return new PriorityBlockingQueue<>(100);
    }

    @Bean(name = "visitedUrl")
    public Set<String> visitedUrl() {
        return Sets.newConcurrentHashSet();
    }

    @Bean
    public RateAccessController rateAccessController() {
        return new RateAccessController();
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public CrawlerCounter crawlerCounter() {
        return new CrawlerCounter() {
            public CrawlerCounter forkTask() {
                return crawlerCounter();
            }
        };
    }
}
