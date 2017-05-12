package ru.shishmakov.config;

import com.google.common.collect.Sets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import ru.shishmakov.core.CrawlerCounter;
import ru.shishmakov.core.RateAccessController;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

/**
 * @author Dmitriy Shishmakov on 12.05.17
 */
@Configuration
public class Config {

    @Bean(name = "wordCounter")
    public ConcurrentMap<String, Long> wordCounter() {
        return new ConcurrentHashMap<>(100);
    }

    @Bean(name = "visitedUrls")
    public Set<String> visitedUrls() {
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
