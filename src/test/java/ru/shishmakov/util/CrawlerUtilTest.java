package ru.shishmakov.util;

import org.aeonbits.owner.ConfigFactory;
import org.junit.Test;
import ru.shishmakov.config.CrawlerConfig;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitriy Shishmakov on 15.05.17
 */
public class CrawlerUtilTest {

    @Test
    public void test() throws Exception {
        CrawlerConfig config = ConfigFactory.create(CrawlerConfig.class);

        assertNotNull("Config should not be null", config);
        assertNotNull("Pattern of illegal symbols should be defined", config.illegalCharacters());
        assertNotNull("Set of acceptable symbols should be defined ", config.acceptableWords());
        assertTrue("Number of symbols should be more than one", config.minAcceptableCountSymbols() > 1);
    }
}
