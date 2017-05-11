package ru.shishmakov;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class UrlTest extends BaseTest {

    @Test
    public void visitedUrlsShouldDefineOnlyUniquePath() throws Exception {
        Set<String> visited = new HashSet<>();
        visited.add("https://yandex.ru");
        List<String> sources = Lists.newArrayList(
                "https://yandex.ru",
                "https://yandex.ru:443/maps/213/moscow/",
                "HTTPS://YANDEX.RU/MAPS/213/MOSCOW/", // equal
                "https://yandex.ru/maps/213/moscow", // equal
                "https://yandex.ru/maps/213/moscow/?rtext=&rtt=auto&mode=routes", // equal
                "https://yandex.ru/maps/43/kazan/?mode=search&text=kazan",
                EMPTY, SPACE, null);
        sources.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(s -> StringUtils.removeEnd(s, "/"))
                .map(StringUtils::lowerCase)
                .forEach(visited::add);

        assertEquals("Invalid number of unique URLs", 4, visited.size());
        visited.forEach(t -> assertTrue("URL should not be blank or null", StringUtils.isNotBlank(t)));
    }
}
