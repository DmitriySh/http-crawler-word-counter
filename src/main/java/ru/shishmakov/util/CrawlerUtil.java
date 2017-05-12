package ru.shishmakov.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class CrawlerUtil {

    public static List<String> parsed(List<String> sources) {
        return sources.stream()
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(s -> StringUtils.removeEnd(s, "/"))
                .map(StringUtils::lowerCase)
                .collect(Collectors.toList());
    }

    public static List<String> getLinks(Document doc) {
        return doc.getElementsByTag("a").stream()
                .map(el -> el.attr("abs:href"))
                .map(StringUtils::trimToEmpty)
                .collect(Collectors.toList());
    }
}
