package ru.shishmakov.core;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ru.shishmakov.BaseTest;

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
public class UrlTest extends BaseTest {

    @Test
    public void visitedUrlsShouldDefineOnlyUniquePath() throws Exception {
        final Set<String> visited = new HashSet<>();
        visited.add("https://yandex.ru");

        List<String> sources = Lists.newArrayList(
                "https://yandex.ru",
                "https://yandex.ru:443/maps/213/moscow/",
                "HTTPS://YANDEX.RU/MAPS/213/MOSCOW/", // equal
                "https://yandex.ru/maps/213/moscow", // equal
                "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes", // equal
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
