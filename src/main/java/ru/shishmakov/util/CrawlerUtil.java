package ru.shishmakov.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.CrawlerConfig;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.*;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class CrawlerUtil {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private CrawlerConfig crawlerConfig;

    private Pattern illegalCharacters;

    @PostConstruct
    public void setUp() {
        illegalCharacters = Pattern.compile(crawlerConfig.illegalCharacters());
    }

    public List<String> simplifyUri(String... sourceUri) {
        checkArgument(sourceUri.length > 0, "list source uri is empty");
        final List<String> temp = new ArrayList<>(sourceUri.length);
        for (String uri : sourceUri) {
            simplifyUri(uri).ifPresent(temp::add);
        }
        return temp;
    }

    public Optional<String> simplifyUri(String sourceUri) {
        return Stream.of(sourceUri)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(s -> StringUtils.removeEnd(s, "/"))
                .map(StringUtils::lowerCase)
                .findFirst();

    }

    public Stream<String> getStreamHrefLinks(Document doc) {
        return checkNotNull(doc, "document is null").getElementsByTag("a").stream()
                .map(el -> el.attr("abs:href"))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull);
    }

    /**
     * Modified version of the method {@link Element#text()}
     * to get list of string instead of a single text block
     */
    public List<String> getText(Element element) {
        checkNotNull(element, "element is null");
        final List<String> data = new ArrayList<>();
        new NodeTraversor(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                Stream.of(node)
                        .filter(n -> n instanceof TextNode)
                        .map(n -> ((TextNode) n).getWholeText())
                        .map(t -> illegalCharacters.matcher(t).replaceAll(""))
                        .map(StringUtils::normalizeSpace)
                        .flatMap(t -> Arrays.stream(split(t, SPACE)))
                        .filter(StringUtils::isNotBlank)
                        .filter(t -> length(t) >= crawlerConfig.minAcceptableCountSymbols() ||
                                crawlerConfig.acceptableWords().contains(t))
                        .forEach(data::add);
            }

            @Override
            public void tail(Node node, int depth) {
                // do nothing
            }
        }).traverse(element);
        return data;
    }
}
