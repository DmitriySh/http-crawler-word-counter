package ru.shishmakov;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.shishmakov.config.AppConfig;
import ru.shishmakov.util.CrawlerUtil;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class JsoupExampleTest extends BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String INNER_DOCUMENT_PATH = "src/test/resources/test_html.html";

    @Inject
    private CrawlerUtil crawlerUtil;

    @Test
    public void parseShouldParseHtmlFromString() {
        String html = "<html>" +
                "<head><title>Text from &lt;Head&gt;</title></head>"
                + "<body><p>Text from <b>&lt;Body&gt;</b></p></body>" +
                "</html>";

        Document doc = Jsoup.parse(html);
        String headTag = doc.getElementsByTag("head").text();
        String bodyTag = doc.getElementsByTag("body").text();
        String headText = doc.head().text();
        String bodyText = doc.body().text();
        logger.info("\nHead: {}\nBody: {}", headText, bodyText);

        assertEquals("Heads are not equal", headTag, headText);
        assertEquals("Body are not equal", bodyTag, bodyText);
    }

    @Test
    public void parsingBodyFragmentShouldParseHtmlFromString() {
        String html = "<div><p>Text from fragment &lt;Div&gt;</p>";

        Document doc = Jsoup.parseBodyFragment(html);
        String fragmentText = doc.body().text();
        String fragmentTag = doc.body().getElementsByTag("div").text();
        logger.info("\nFragment: {}", fragmentText);

        assertEquals("Fragments are not equal", fragmentTag, fragmentText);
    }

    @Test
    public void connectShouldCreateHttpConnectionAndLoadHtmlByUrl() throws IOException {
        Document doc = Jsoup.connect("https://yandex.ru")
                .userAgent("Mozilla/5.0")
                .timeout(3_000)
                .get();
        String headText = doc.head().text();
        String bodyText = doc.body().text();
        logger.info("\nSite head text: {}", headText);
        logger.info("\nSite body text: {}", bodyText);

        assertTrue("Head is not empty", StringUtils.isNotEmpty(headText));
        assertTrue("Body is not empty", StringUtils.isNotEmpty(bodyText));
    }

    @Test
    public void parseShouldLoadHtmlByInnerPath() throws Exception {
        File input = new File(INNER_DOCUMENT_PATH);
        Document doc = Jsoup.parse(input, "UTF-8");
        String headText = doc.head().text();
        String bodyText = doc.body().text();
        logger.info("\nSite head text: {}", headText);
        logger.info("\nSite body text: {}", bodyText);

        assertTrue("Head is not empty", StringUtils.isNotEmpty(headText));
        assertTrue("Body is not empty", StringUtils.isNotEmpty(bodyText));
    }

    @Test
    public void parseShouldCreateHtmlDocumentAndResolveRelativeUrls() throws Exception {
        String baseUri = "http://jsoup.org";
        File input = new File(INNER_DOCUMENT_PATH);
        Document doc = Jsoup.parse(input, "UTF-8", baseUri);
        List<String> links = crawlerUtil.getStreamHrefLinks(doc).collect(Collectors.toList());
        logger.info("\nSite links: {}", links);

        assertFalse("List of links is not empty", links.isEmpty());
        links.forEach(l -> assertTrue("Link is illegal", StringUtils.startsWithIgnoreCase(l, baseUri)));
    }

    @Test
    public void setBaseUriShouldResolveDocumentRelativeUrls() throws Exception {
        String baseUri = "http://jsoup.org";
        File input = new File(INNER_DOCUMENT_PATH);
        Document doc = Jsoup.parse(input, "UTF-8");
        doc.setBaseUri(baseUri);
        List<String> links = crawlerUtil.getStreamHrefLinks(doc).collect(Collectors.toList());
        logger.info("\nSite links: {}", links);

        assertFalse("List of links is not empty", links.isEmpty());
        links.forEach(l -> assertTrue("Link is illegal", StringUtils.startsWithIgnoreCase(l, baseUri)));
    }


}
