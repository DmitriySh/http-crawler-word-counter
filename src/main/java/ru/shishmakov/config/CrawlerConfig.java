package ru.shishmakov.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

import java.util.Set;

/**
 * @author Dmitriy Shishmakov on 15.05.17
 */
@Sources({"file:config/crawler.properties", "classpath:config/crawler.properties"})
public interface CrawlerConfig extends Config {

    @DefaultValue("я, мы, ты, вы, он, она, оно, они, i, am, you, are, he, she, it, we, they")
    @Key("legal.words")
    Set<String> acceptableWords();

    @DefaultValue("3")
    @Key("legal.minSymbols")
    int minAcceptableCountSymbols();

    @DefaultValue("[\\|\\«\\»\'\"\\!\\?\\.\\:\\;\\,\\[\\]{}()+/\\\\]")
    @Key("illegal.pattern")
    String illegalCharactersPattern();

    @DefaultValue("20")
    @Key("rps")
    int requestPerSecond();

    @DefaultValue("100")
    @Key("top.count")
    int topRating();

    @DefaultValue("3000")
    @Key("request.timeout")
    int requestTimeoutMs();
}
