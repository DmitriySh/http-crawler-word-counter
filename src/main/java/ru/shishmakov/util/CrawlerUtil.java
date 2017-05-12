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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class CrawlerUtil {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Need to fill unwanted symbols by your choice
     */
    private static final Pattern UNWANTED_SYMBOLS = Pattern.compile("([\\·\\|\\©\\«\\»\'\"\\!\\?\\.\\:\\;\\,\\[\\]{}()+/\\\\])");

    public static List<String> simplifyUri(String... sourceUri) {
        checkArgument(sourceUri.length > 0, "list source uri is empty");
        final List<String> temp = new ArrayList<>(sourceUri.length);
        for (String uri : sourceUri) {
            simplifyUri(uri).ifPresent(temp::add);
        }
        return temp;
    }

    public static Optional<String> simplifyUri(String sourceUri) {
        return Stream.of(sourceUri)
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .map(s -> StringUtils.substringBefore(s, "?"))
                .map(s -> StringUtils.removeEnd(s, "/"))
                .map(StringUtils::lowerCase)
                .findFirst();

    }

    public static Stream<String> getStreamHrefLinks(Document doc) {
        return checkNotNull(doc, "document is null").getElementsByTag("a").stream()
                .map(el -> el.attr("abs:href"))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull);
    }

    /**
     * Modified version of the method {@link Element#text()}
     * to get list of string instead of a single text block
     */
    public static List<String> getText(Element element) {
        checkNotNull(element, "element is null");
        final List<String> data = new ArrayList<>();
        new NodeTraversor(new NodeVisitor() {
            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    String text = StringUtils.trimToNull(textNode.getWholeText());
                    if (StringUtils.isNotBlank(text)) {
                        Matcher matcher = UNWANTED_SYMBOLS.matcher(text);
                        for (String str : StringUtils.split(matcher.replaceAll(""), SPACE)) {
                            str = StringUtils.normalizeSpace(str);
                            if (StringUtils.isNotBlank(str)) {
                                data.add(str);
                            }
                        }
                    }
                }
            }

            public void tail(Node node, int depth) {
                // do nothing
            }
        }).traverse(element);
        return data;
    }
}
