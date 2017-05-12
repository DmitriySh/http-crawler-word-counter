package ru.shishmakov;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.shishmakov.util.CrawlerUtil.simplifyUri;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class UriTest extends BaseTest {

    @Test
    public void simplifyUriShouldNormalizeUniquePathsAndFilterBlank() throws Exception {
        final ArrayList<String> expectedUri = Lists.newArrayList("https://yandex.ru",
                "https://yandex.ru",
                "https://yandex.ru",
                "https://yandex.ru:443/maps/213/moscow",
                "https://yandex.ru/maps/213/moscow",
                "https://yandex.ru/maps/213/moscow",
                "https://yandex.ru/maps/213/moscow",
                "https://yandex.ru/maps/43/kazan");

        final List<String> parsedUri = simplifyUri(
                "https://yandex.ru/", // equal 1
                "https://yandex.ru", // equal 1
                "https://yandex.ru:443/maps/213/moscow/",
                "HTTPS://YANDEX.RU/MAPS/213/MOSCOW/", // equal 2
                "https://yandex.ru/maps/213/moscow", // equal 2
                "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes", // equal 2
                "https://yandex.ru/maps/43/kazan/?mode=search&text=kazan",
                EMPTY, SPACE, null);

        assertEquals("Invalid number of unique URLs", new HashSet<>(expectedUri).size(), new HashSet<>(parsedUri).size());
        assertTrue("Invalid number of unique URLs", parsedUri.stream().allMatch(expectedUri::contains));
    }

    @Test
    public void baseUriShouldBeEqual() throws Exception {
        final String sourceUrl = "http://jsoup.org";
        final URL url = new URL(sourceUrl);
        final String baseUri = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();

        assertEquals("Base uri should be equal", sourceUrl, baseUri);
    }
}
