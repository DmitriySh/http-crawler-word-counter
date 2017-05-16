package ru.shishmakov;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.shishmakov.config.AppConfig;
import ru.shishmakov.util.CrawlerUtil;

import javax.inject.Inject;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UriTest extends BaseTest {

    @Inject
    private CrawlerUtil crawlerUtil;

    @Test
    public void simplifyUriShouldNormalizeUniquePathsAndFilterBlank() throws Exception {
        final List<String> expectedUri = Lists.newArrayList(
                "https://yandex.ru",
                "https://yandex.ru:443/maps/213/moscow",
                "https://yandex.ru/maps/213/moscow",
                "https://yandex.ru/maps/43/kazan");

        final List<String> parsedUri = crawlerUtil.simplifyUri(
                "https://yandex.ru/", // equal 1
                "https://yandex.ru", // equal 1
                "https://yandex.ru:443/maps/213/moscow/",
                "HTTPS://YANDEX.RU/MAPS/213/MOSCOW/", // equal 2
                "https://yandex.ru/maps/213/moscow", // equal 2
                "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes", // equal 2
                "https://yandex.ru/maps/43/kazan/?mode=search&text=kazan",
                EMPTY, SPACE, null);

        assertEquals("Invalid number of unique URLs", expectedUri.size(), new HashSet<>(parsedUri).size());
        assertTrue("Invalid number of unique URLs", parsedUri.stream().allMatch(expectedUri::contains));
    }

    @Test
    public void baseUriShouldBeEqual() throws Exception {
        final String sourceUrl = "http://jsoup.org";
        URI uri = new URI(sourceUrl);
        String baseUri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null).toString();
        assertEquals("Base uri should be equal", sourceUrl, baseUri);
    }

    @Test
    public void name() throws Exception {
        String source = "https://yandex.ru/sub1/sub2/../../moscow";
        String source2 = "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes";
        URI uri = new URI(source);
        URI uriRelative = new URI("../../moscow");
        URL url = new URL(source);
        URL url2 = new URL(source2);
        URI uri2 = new URI(source2);
        final String baseUri = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();

        logger.info("baseUri: {}", baseUri);

        logger.info("uri: {}", uri);
        logger.info("uri normalize: {}", uri.normalize());
        logger.info("uriRelative normalize: {}", uriRelative.normalize());
        logger.info("uriRelative resolve: {}", uriRelative.resolve("https://yandex.ru/sub1/sub2"));
        logger.info("uriRelative relativize: {}", uriRelative.relativize(URI.create("https://yandex.ru/sub1/sub2")));


        logger.info("url: {}", url);
        logger.info("url toExternalForm: {}", url.toExternalForm());
    }
}
