package ru.shishmakov.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

/**
 * @author Dmitriy Shishmakov on 15.05.17
 */
@Sources({"file:config/crawler.properties", "classpath:config/crawler.properties"})
public interface CrawlerConfig extends Config {

    @DefaultValue("я, мы, ты, вы, он, она, оно, они")
    @Key("acceptableWords")
    String[] acceptableWords();

    @DefaultValue("3")
    @Key("minSymbols")
    long minAcceptableCountSymbols();
}
