package ru.shishmakov;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.shishmakov.util.CrawlerUtil.parsed;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class UriTest extends BaseTest {

    @Test
    public void visitedUrlsShouldDefineOnlyUniquePath() throws Exception {
        final Set<String> visited = new HashSet<>();
        List<String> sources = Lists.newArrayList(
                "https://yandex.ru/", // equal 1
                "https://yandex.ru", // equal 1
                "https://yandex.ru:443/maps/213/moscow/",
                "HTTPS://YANDEX.RU/MAPS/213/MOSCOW/", // equal 2
                "https://yandex.ru/maps/213/moscow", // equal 2
                "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes", // equal 2
                "https://yandex.ru/maps/43/kazan/?mode=search&text=kazan",
                EMPTY, SPACE, null);
        List<String> parsedUrls = parsed(sources);
        visited.addAll(parsedUrls);

        assertEquals("Invalid number of unique URLs", 4, visited.size());
        visited.forEach(t -> assertTrue("URL should not be blank or null", StringUtils.isNotBlank(t)));
    }

    @Test
    public void baseUriShouldBeEqual() throws Exception {
        final String sourceUrl = "http://jsoup.org";
        final URL url = new URL(sourceUrl);
        final String baseUri = new URL(url.getProtocol(), url.getHost(), url.getPort(), "").toString();

        assertEquals("Base uri should be equal", sourceUrl, baseUri);
    }
}
