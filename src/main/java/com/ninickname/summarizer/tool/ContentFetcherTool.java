package com.ninickname.summarizer.tool;

import com.ninickname.summarizer.model.ContentData;
import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ContentFetcherTool {
    private static final Logger logger = LoggerFactory.getLogger(ContentFetcherTool.class);
    private static final int MAX_CONTENT_LENGTH = 15000;
    private static final int MAX_SECTION_LENGTH = 3000; // Increased for Wikipedia and long articles
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ExecutorService executorService;

    public ContentFetcherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public ContentFetcherTool(int threadPoolSize) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }


    @Tool("Fetch structured content from URLs in parallel")
    public List<ContentData> fetchMultipleStructuredContents(List<String> urls) {
        logger.info("ContentFetcherTool: Fetching structured content from {} URLs", urls.size());
        try {
            List<CompletableFuture<ContentData>> futures = urls.stream()
                    .map(this::fetchStructuredContentAsync)
                    .toList();

            List<ContentData> contents = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(content -> content != null && !content.sections().isEmpty())
                    .toList();

            logger.info("ContentFetcherTool: Successfully fetched {} structured contents", contents.size());
            return contents;
        } catch (Exception e) {
            logger.error("Failed to fetch structured content from {} URLs: {}", urls.size(), e.getMessage(), e);
            throw new RuntimeException("Content fetching failed for " + urls.size() + " URLs: " + e.getMessage(), e);
        }
    }


    private CompletableFuture<ContentData> fetchStructuredContentAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchStructuredContent(url);
            } catch (Exception e) {
                logger.warn("Failed to fetch structured content from {}: {}", url, e.getMessage());
                throw new CompletionException(e);
            }
        }, executorService).exceptionally(throwable -> {
            logger.warn("Async fetch failed for {}: {}", url, throwable.getMessage());
            return null;
        });
    }

    private CompletableFuture<String> fetchContentAsync(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchContent(url);
            } catch (Exception e) {
                logger.warn("Failed to fetch content from {}: {}", url, e.getMessage());
                throw new CompletionException(e);
            }
        }, executorService).exceptionally(throwable -> {
            logger.warn("Async fetch failed for {}: {}", url, throwable.getMessage());
            return null;
        });
    }

    /**
     * Check if URL should be skipped due to known limitations
     */
    private boolean shouldSkipUrl(String url) {
        // O'Reilly library views - unreliable, may require authentication
        if (url.contains("oreilly.com/library/view/")) {
            return true;
        }

        // IBM docs - uses JavaScript rendering
        if (url.contains("ibm.com/docs/")) {
            return true;
        }

        // Paywalled news sites - require subscription
        if (url.contains("nytimes.com/") || url.contains("wsj.com/") ||
            url.contains("ft.com/content/") || url.contains("economist.com/")) {
            return true;
        }

        // Medium member-only posts
        if (url.contains("medium.com/") && url.contains("/p/")) {
            return true;
        }

        // Video/media sites - minimal text content
        if (url.contains("youtube.com") || url.contains("youtu.be") ||
            url.contains("vimeo.com") || url.contains("tiktok.com") ||
            url.contains("twitch.tv")) {
            return true;
        }

        // Social media - poor content extraction
        if (url.contains("twitter.com") || url.contains("x.com") ||
            url.contains("facebook.com") || url.contains("instagram.com") ||
            url.contains("linkedin.com/posts/")) {
            return true;
        }

        // PDF files - need different parser
        if (url.endsWith(".pdf")) {
            return true;
        }

        // Presentation/slide platforms - minimal extractable text
        if (url.contains("slideshare.net") || url.contains("slides.com") ||
            url.contains("prezi.com") || url.contains("speakerdeck.com")) {
            return true;
        }

        return false;
    }

    private ContentData fetchStructuredContent(String url) throws IOException, InterruptedException {
        // Skip unreliable or unsupported sites
        if (shouldSkipUrl(url)) {
            logger.warn("Skipping unsupported/unreliable URL: {}", url);
            return null;
        }

        // Rewrite Reddit URLs to use old.reddit.com for full HTML content
        String fetchUrl = url;
        if (url.contains("reddit.com") && !url.contains("old.reddit.com")) {
            fetchUrl = url.replace("www.reddit.com", "old.reddit.com")
                          .replace("reddit.com", "old.reddit.com");
            logger.debug("Rewritten Reddit URL: {} -> {}", url, fetchUrl);
        }

        logger.debug("Fetching structured content from: {}", fetchUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fetchUrl))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                logger.warn("HTTP error {} for URL: {}", response.statusCode(), url);
                return null;
            }

            String html = response.body();
            if (html == null || html.isEmpty()) {
                return null;
            }

            return extractStructuredContent(html, url);
        } catch (Exception e) {
            logger.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String fetchContent(String url) throws IOException, InterruptedException {
        ContentData content = fetchStructuredContent(url);
        return content != null ? com.ninickname.summarizer.formatter.StructuredContentFormatter.toFormattedString(content) : null;
    }

    private ContentData extractStructuredContent(String html, String url) {
        try {
            Document doc = Jsoup.parse(html);

            // Extract metadata using helper methods
            String title = extractTitle(doc);
            String mainHeading = extractMainHeading(doc);
            Element mainElement = findMainContent(doc);

            // Extract sections - try h2 first, then h3 (for Medium and similar sites)
            // First try within mainElement, but if no headings found, search entire body
            // (some sites have content spread across multiple divs)
            Elements h2Headings = mainElement.select("h2");
            Elements h3Headings = mainElement.select("h3");

            logger.info("Found {} h2 and {} h3 headings in mainElement for {}",
                    h2Headings.size(), h3Headings.size(), url);
            if (!h2Headings.isEmpty()) {
                logger.info("H2 headings: {}", h2Headings.stream().map(Element::text).limit(5).toList());
            }

            // If no headings found in mainElement, try searching the whole body
            if (h2Headings.isEmpty() && h3Headings.isEmpty()) {
                Element body = doc.body();
                if (body != null) {
                    h2Headings = body.select("h2");
                    h3Headings = body.select("h3");
                    // If we found headings in body, use body as mainElement
                    if (!h2Headings.isEmpty() || !h3Headings.isEmpty()) {
                        logger.info("No headings in mainElement, using body instead (found {} h2, {} h3)",
                                h2Headings.size(), h3Headings.size());
                        mainElement = body;
                    }
                }
            }

            List<ContentData.SectionData> sectionDataList = null;
            boolean hasStructure = false;

            // Smart heading selection: prefer h3 if significantly more h3s than h2s
            // or if h2 headings are repetitive (likely navigation/forms)
            Elements selectedHeadings = null;
            if (!h2Headings.isEmpty() && !h3Headings.isEmpty()) {
                // Check if h2s are repetitive (same text appearing multiple times)
                long uniqueH2Count = h2Headings.stream()
                        .map(Element::text)
                        .distinct()
                        .count();
                boolean h2sRepetitive = uniqueH2Count < h2Headings.size() * 0.7; // Less than 70% unique

                // Prefer h3 if: h3s outnumber h2s by 3x OR h2s are repetitive
                if (h3Headings.size() >= h2Headings.size() * 3 || h2sRepetitive) {
                    logger.info("Using h3 headings instead of h2 ({} h3s vs {} h2s, h2s repetitive: {})",
                            h3Headings.size(), h2Headings.size(), h2sRepetitive);
                    selectedHeadings = h3Headings;
                } else {
                    selectedHeadings = h2Headings;
                }
            } else if (!h2Headings.isEmpty()) {
                selectedHeadings = h2Headings;
            } else if (!h3Headings.isEmpty()) {
                selectedHeadings = h3Headings;
            } else {
                hasStructure = false;
                sectionDataList = extractUnstructuredSection(mainElement);
            }

            if (selectedHeadings != null) {
                hasStructure = true;
                sectionDataList = extractStructuredSections(selectedHeadings, mainElement);
            }

            // Calculate total characters (including subsections)
            int totalChars = sectionDataList.stream()
                    .mapToInt(s -> {
                        int sectionContent = s.content() != null ? s.content().length() : 0;
                        int subSectionContent = s.subSections().stream()
                                .mapToInt(sub -> sub.content() != null ? sub.content().length() : 0)
                                .sum();
                        return sectionContent + subSectionContent;
                    })
                    .sum();

            // Build the immutable ContentData record
            ContentData contentData = new ContentData(
                    url,
                    title,
                    mainHeading,
                    sectionDataList,
                    totalChars,
                    hasStructure,
                    null, // engine
                    null  // score
            );

            logger.debug("Extracted structured content from {}: {} sections, {} chars, hasStructure={}",
                    url, sectionDataList.size(), totalChars, hasStructure);

            return contentData;

        } catch (Exception e) {
            logger.warn("Failed to parse structured content from {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String elementToMarkdown(Element element) {
        if (element == null) return "";

        String tag = element.tagName();
        StringBuilder md = new StringBuilder();

        switch (tag) {
            case "p":
                md.append(processInlineElements(element));
                break;

            case "h1":
                md.append("# ").append(element.text());
                break;
            case "h2":
                md.append("## ").append(element.text());
                break;
            case "h3":
                md.append("### ").append(element.text());
                break;
            case "h4":
                md.append("#### ").append(element.text());
                break;
            case "h5":
                md.append("##### ").append(element.text());
                break;
            case "h6":
                md.append("###### ").append(element.text());
                break;

            case "ul":
                for (Element li : element.select("> li")) {
                    String liContent = processInlineElements(li).trim();
                    if (!liContent.isEmpty()) {  // Only add non-empty list items
                        md.append("- ").append(liContent).append("\n");
                    }
                }
                if (md.length() > 0 && md.charAt(md.length() - 1) == '\n') {
                    md.setLength(md.length() - 1); // Remove trailing newline
                }
                break;

            case "ol":
                int index = 1;
                for (Element li : element.select("> li")) {
                    String liContent = processInlineElements(li).trim();
                    if (!liContent.isEmpty()) {  // Only add non-empty list items
                        md.append(index++).append(". ").append(liContent).append("\n");
                    }
                }
                if (md.length() > 0 && md.charAt(md.length() - 1) == '\n') {
                    md.setLength(md.length() - 1);
                }
                break;

            case "blockquote":
                String quote = element.text();
                for (String line : quote.split("\n")) {
                    md.append("> ").append(line).append("\n");
                }
                if (md.length() > 0 && md.charAt(md.length() - 1) == '\n') {
                    md.setLength(md.length() - 1);
                }
                break;

            case "pre":
                Element code = element.selectFirst("code");
                if (code != null) {
                    md.append("```\n").append(code.text()).append("\n```");
                } else {
                    md.append("```\n").append(element.text()).append("\n```");
                }
                break;

            case "code":
                if (element.parent() != null && !element.parent().tagName().equals("pre")) {
                    md.append("`").append(element.text()).append("`");
                }
                break;

            case "a":
                String href = element.attr("href");
                String linkText = element.text();
                if (!href.isEmpty()) {
                    md.append("[").append(linkText).append("](").append(href).append(")");
                } else {
                    md.append(linkText);
                }
                break;

            case "br":
                md.append("  \n"); // Two spaces + newline = line break in markdown
                break;

            case "hr":
                md.append("---");
                break;

            case "table":
                md.append(tableToMarkdown(element));
                break;

            case "dl":
                // Definition lists
                Elements dts = element.select("dt");
                Elements dds = element.select("dd");
                for (int i = 0; i < Math.min(dts.size(), dds.size()); i++) {
                    md.append("**").append(dts.get(i).text()).append("**\n");
                    md.append(": ").append(dds.get(i).text()).append("\n\n");
                }
                if (md.length() > 0) {
                    md.setLength(md.length() - 2); // Remove trailing newlines
                }
                break;

            default:
                // For div, span, and other containers, process children
                if (element.children().isEmpty()) {
                    String text = element.text().trim();
                    if (!text.isEmpty()) {
                        md.append(text);
                    }
                } else {
                    for (Element child : element.children()) {
                        String childMd = elementToMarkdown(child);
                        if (!childMd.isEmpty()) {
                            md.append(childMd).append("\n\n");
                        }
                    }
                    if (md.length() > 1 && md.substring(md.length() - 2).equals("\n\n")) {
                        md.setLength(md.length() - 2);
                    }
                }
                break;
        }

        return md.toString();
    }

    private String processInlineElements(Element element) {
        StringBuilder result = new StringBuilder();

        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode) {
                result.append(((org.jsoup.nodes.TextNode) node).text());
            } else if (node instanceof Element) {
                Element el = (Element) node;
                String tag = el.tagName();

                switch (tag) {
                    case "strong", "b":
                        result.append("**").append(el.text()).append("**");
                        break;
                    case "em", "i":
                        result.append("*").append(el.text()).append("*");
                        break;
                    case "code":
                        result.append("`").append(el.text()).append("`");
                        break;
                    case "a":
                        String href = el.attr("href");
                        if (!href.isEmpty()) {
                            result.append("[").append(el.text()).append("](").append(href).append(")");
                        } else {
                            result.append(el.text());
                        }
                        break;
                    case "br":
                        result.append("  \n");
                        break;
                    default:
                        result.append(el.text());
                        break;
                }
            }
        }

        return result.toString().trim();
    }

    private String tableToMarkdown(Element table) {
        StringBuilder md = new StringBuilder();

        Elements rows = table.select("tr");
        if (rows.isEmpty()) return "";

        // Header row
        Element headerRow = rows.first();
        Elements headers = headerRow.select("th");
        if (headers.isEmpty()) {
            headers = headerRow.select("td");
        }

        if (!headers.isEmpty()) {
            md.append("| ");
            for (Element header : headers) {
                md.append(header.text()).append(" | ");
            }
            md.append("\n|");
            for (int i = 0; i < headers.size(); i++) {
                md.append(" --- |");
            }
            md.append("\n");

            // Data rows
            for (int i = 1; i < rows.size(); i++) {
                Elements cells = rows.get(i).select("td");
                if (!cells.isEmpty()) {
                    md.append("| ");
                    for (Element cell : cells) {
                        md.append(cell.text()).append(" | ");
                    }
                    md.append("\n");
                }
            }
        }

        return md.toString();
    }

    private String extractTextFromHtml(String html, String url) {
        ContentData content = extractStructuredContent(html, url);
        return content != null ? com.ninickname.summarizer.formatter.StructuredContentFormatter.toFormattedString(content) : null;
    }

    private String cleanWikipediaIntro(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Remove Wikipedia-specific prefixes
        text = text.replaceAll("^\\[\\]\\(/wiki/[^)]+\\)\\s*", ""); // Remove leading [](/wiki/...) links
        text = text.replaceAll("^From Wikipedia, the free encyclopedia\\s*", "");
        text = text.replaceAll("^This article is about .+?\\. For .+?\\s*", ""); // Disambiguation notes
        text = text.replaceAll("^Not to be confused with .+?\\s*", "");
        text = text.replaceAll("^\\[edit\\]\\s*", "");

        // Remove lines that are just navigation
        String[] lines = text.split("\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip Wikipedia navigation lines
            if (trimmed.startsWith("[](/wiki/") && trimmed.length() < 100) {
                continue;
            }
            if (trimmed.equals("From Wikipedia, the free encyclopedia")) {
                continue;
            }
            if (cleaned.length() > 0) {
                cleaned.append("\n");
            }
            cleaned.append(line);
        }

        return cleaned.toString().trim();
    }

    private String stripJunkFromEnds(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Remove junk from the very beginning - be conservative, only exact matches
        // Keep removing lines until we hit real content
        String[] lines = text.split("\\n");
        int startIndex = 0;

        for (int i = 0; i < lines.length && i < 20; i++) {  // Check first 20 lines max
            String line = lines[i].trim();
            if (line.isEmpty() ||
                line.matches("Last Updated.*") ||
                line.equals("Comments") ||
                line.equals("Improve") ||
                line.equals("Suggest changes") ||
                line.equals("Like Article") ||
                line.equals("Like") ||
                line.equals("Report") ||
                line.equals("Listen") ||
                line.equals("Share") ||
                line.matches("\\d+\\s+min\\s+read") ||  // Medium "7 min read"
                line.matches("\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*\\d{4}.*") ||
                line.matches("\\[.*\\]\\(/@.*\\)")) {  // Medium author links like [Name](/@username)
                startIndex = i + 1;
            } else {
                break;  // Hit real content, stop
            }
        }

        // Remove junk from the very end
        int endIndex = lines.length;
        for (int i = lines.length - 1; i >= 0 && i >= lines.length - 30; i--) {  // Check last 30 lines max
            String line = lines[i].trim();
            if (line.isEmpty() ||
                line.equals("Follow") ||
                line.equals("Improve") ||
                line.startsWith("Article Tags") ||
                line.startsWith("- [") ||  // List items that are just links
                line.matches("\\[.\\]\\(https://.*\\)") ||  // Single letter links like [A](url)
                line.matches("\\[.*\\]\\(https://www\\.geeksforgeeks\\.org/user/.*\\)") ||
                line.matches("\\[.*\\]\\(https://www\\.geeksforgeeks\\.org/category/.*\\)") ||
                line.matches("\\[.*\\]\\(https://www\\.geeksforgeeks\\.org/tag/.*\\)")) {
                endIndex = i;
            } else {
                break;  // Hit real content, stop
            }
        }

        // Reconstruct text from cleaned lines
        if (startIndex < endIndex) {
            StringBuilder cleaned = new StringBuilder();
            for (int i = startIndex; i < endIndex; i++) {
                cleaned.append(lines[i]);
                if (i < endIndex - 1) cleaned.append("\n");
            }
            return cleaned.toString().trim();
        }

        return text.trim();
    }

    // Section extraction methods
    private List<ContentData.SectionData> extractStructuredSections(Elements headings, Element mainElement) {
        List<ContentData.SectionData> sections = new ArrayList<>();

        // First, extract any content BEFORE the first heading (introduction)
        if (!headings.isEmpty()) {
            Element firstHeading = headings.first();
            StringBuilder introContent = new StringBuilder();

            // Get all children of mainElement before the first heading
            for (Element child : mainElement.children()) {
                if (child.equals(firstHeading)) {
                    break;
                }
                // Skip if it's a heading itself
                if (child.tagName().matches("h[1-6]")) {
                    continue;
                }
                String markdown = elementToMarkdown(child);
                if (!markdown.isEmpty()) {
                    introContent.append(markdown).append("\n\n");
                }
            }

            // Add introduction section if it has meaningful content
            String introText = introContent.toString().trim();

            // Clean Wikipedia-specific junk from introduction
            introText = cleanWikipediaIntro(introText);

            if (!introText.isEmpty() && introText.length() >= 50) { // At least 50 chars to be meaningful
                if (introText.length() > MAX_SECTION_LENGTH) {
                    introText = introText.substring(0, MAX_SECTION_LENGTH) + "...";
                }
                sections.add(new ContentData.SectionData("Introduction", introText, List.of()));
            }
        }

        // Now extract regular sections from headings
        for (Element heading : headings) {
            ContentData.SectionData section = extractSingleSection(heading);

            // Calculate total section chars (content + subsections)
            int sectionContentChars = section.content() != null ? section.content().length() : 0;
            int subSectionChars = section.subSections().stream()
                    .mapToInt(sub -> sub.content() != null ? sub.content().length() : 0)
                    .sum();
            int totalSectionChars = sectionContentChars + subSectionChars;

            // Only add sections with meaningful content (at least 50 chars total)
            if (totalSectionChars >= 50) {
                sections.add(section);
            }
        }

        return sections;
    }

    private ContentData.SectionData extractSingleSection(Element h2) {
        String heading = h2.text();

        // For pages with complex nesting (like Elementor), try nextElementSibling first
        // If that doesn't work (no siblings), fall back to collecting all text until next h2
        Element current = h2.nextElementSibling();

        // Check if there are actual siblings
        boolean hasSiblings = false;
        Element test = current;
        while (test != null && !test.tagName().equals("h2")) {
            if (!test.text().trim().isEmpty()) {
                hasSiblings = true;
                break;
            }
            test = test.nextElementSibling();
        }

        if (!hasSiblings) {
            // No siblings - this is a nested layout, use alternative extraction
            return extractSectionByTextPosition(h2);
        }

        // Standard sibling-based extraction
        StringBuilder sectionContent = new StringBuilder();
        List<ContentData.SubSectionData> subSections = new ArrayList<>();

        StringBuilder currentSubSectionContent = new StringBuilder();
        String currentSubSectionHeading = null;

        while (current != null && !current.tagName().equals("h2")) {
            // Check if this is a subsection heading (h3 or h4)
            if (current.tagName().equals("h3") || current.tagName().equals("h4")) {
                // Save previous subsection if exists
                if (currentSubSectionHeading != null && currentSubSectionContent.length() > 0) {
                    subSections.add(createSubSection(currentSubSectionHeading, currentSubSectionContent.toString()));
                }

                // Start new subsection
                currentSubSectionHeading = current.text();
                currentSubSectionContent = new StringBuilder();

            } else {
                // Convert element to markdown
                String markdown = elementToMarkdown(current);
                if (!markdown.isEmpty()) {
                    if (currentSubSectionHeading != null) {
                        // Content belongs to subsection
                        currentSubSectionContent.append(markdown).append("\n\n");
                    } else {
                        // Content belongs to main section (before any subsection)
                        sectionContent.append(markdown).append("\n\n");
                    }
                }
            }
            current = current.nextElementSibling();
        }

        // Save last subsection if exists
        if (currentSubSectionHeading != null && currentSubSectionContent.length() > 0) {
            subSections.add(createSubSection(currentSubSectionHeading, currentSubSectionContent.toString()));
        }

        // Build section content
        String contentText = sectionContent.toString().trim();
        if (contentText.length() > MAX_SECTION_LENGTH) {
            contentText = contentText.substring(0, MAX_SECTION_LENGTH) + "...";
        }

        return new ContentData.SectionData(heading, contentText, subSections);
    }

    /**
     * Alternative extraction for heavily nested layouts (like Elementor)
     * Extracts all paragraphs/content between current h2 and next h2 in document order
     */
    private ContentData.SectionData extractSectionByTextPosition(Element h2) {
        String heading = h2.text();
        Element parent = h2.parent();
        while (parent != null && !parent.tagName().equals("body")) {
            parent = parent.parent();
        }

        if (parent == null) {
            return new ContentData.SectionData(heading, "", List.of());
        }

        // Get all h2s and all paragraphs/content elements
        Elements allH2s = parent.select("h2");
        Elements allContent = parent.select("p, ul, ol, blockquote, pre, table");

        // Find index of current h2
        int currentIndex = -1;
        for (int i = 0; i < allH2s.size(); i++) {
            if (allH2s.get(i).equals(h2)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return new ContentData.SectionData(heading, "", List.of());
        }

        // Determine next h2 (if exists)
        Element nextH2 = currentIndex + 1 < allH2s.size() ? allH2s.get(currentIndex + 1) : null;

        // Collect all content between current h2 and next h2
        StringBuilder sectionContent = new StringBuilder();
        for (Element content : allContent) {
            // Check if this content comes after current h2
            if (isAfter(content, h2, parent)) {
                // Check if it comes before next h2 (or no next h2)
                if (nextH2 == null || isBefore(content, nextH2, parent)) {
                    String markdown = elementToMarkdown(content);
                    if (!markdown.isEmpty()) {
                        sectionContent.append(markdown).append("\n\n");
                    }
                }
            }
        }

        String contentText = sectionContent.toString().trim();
        if (contentText.length() > MAX_SECTION_LENGTH) {
            contentText = contentText.substring(0, MAX_SECTION_LENGTH) + "...";
        }

        return new ContentData.SectionData(heading, contentText, List.of());
    }

    /**
     * Check if element1 appears before element2 in document order
     */
    private boolean isBefore(Element element1, Element element2, Element root) {
        String html = root.html();
        int pos1 = html.indexOf(element1.outerHtml());
        int pos2 = html.indexOf(element2.outerHtml());
        return pos1 < pos2 && pos1 != -1 && pos2 != -1;
    }

    /**
     * Check if element1 appears after element2 in document order
     */
    private boolean isAfter(Element element1, Element element2, Element root) {
        return isBefore(element2, element1, root);
    }

    private ContentData.SubSectionData createSubSection(String heading, String content) {
        String trimmedContent = content.trim();
        if (trimmedContent.length() > MAX_SECTION_LENGTH) {
            trimmedContent = trimmedContent.substring(0, MAX_SECTION_LENGTH) + "...";
        }

        return new ContentData.SubSectionData(heading, trimmedContent);
    }

    private List<ContentData.SectionData> extractUnstructuredSection(Element mainElement) {
        StringBuilder markdown = new StringBuilder();
        for (Element child : mainElement.children()) {
            String md = elementToMarkdown(child);
            if (!md.isEmpty()) {
                markdown.append(md).append("\n\n");
            }
        }

        String contentText = markdown.toString().trim();

        // Apply stripJunkFromEnds - only at very beginning and very end
        contentText = stripJunkFromEnds(contentText);

        if (contentText.length() > MAX_CONTENT_LENGTH) {
            contentText = contentText.substring(0, MAX_CONTENT_LENGTH) + "...";
        }

        ContentData.SectionData section = new ContentData.SectionData("Main Content", contentText, List.of());
        return List.of(section);
    }

    // Helper extraction methods
    private String extractTitle(Document doc) {
        String title = doc.title();
        return title != null && !title.isEmpty() ? title : "Untitled";
    }

    private String extractMainHeading(Document doc) {
        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            String heading = h1.text();
            h1.remove(); // Remove to avoid duplication
            return heading;
        }
        return null;
    }

    private Element findMainContent(Document doc) {
        // Remove noise elements FIRST
        doc.select("script, style, nav, footer, aside, " +
                ".header:not(.article-header), .footer, .navigation, .nav, .menu, " +
                ".sidebar, .advertisement, .ad, .ads, " +
                ".social-share, .share, " +
                ".cookie-banner, .cookie-notice, " +
                ".popup, .modal, .overlay, " +
                ".breadcrumb, .related, .recommended, " +
                ".dropdown-title, .dropdown-item").remove();

        // Wikipedia-specific cleanup
        doc.select(".navbox, .vertical-navbox, .sistersitebox, " +
                ".metadata, .ambox, .mbox-small, " +
                ".infobox, .toc, #toc, " +
                ".reflist, .reference, .mw-editsection, " +
                ".noprint, .catlinks, .printfooter, " +
                "#mw-navigation, #mw-indicator-pp-default, " +
                ".hatnote, .dablink").remove();

        // Try multiple selectors in priority order
        Element mainElement = null;

        // Wikipedia-specific selector
        if (doc.location().contains("wikipedia.org")) {
            mainElement = doc.selectFirst(".mw-parser-output");
            if (mainElement != null) {
                // Remove Wikipedia-specific junk from the beginning
                mainElement.select(".mw-empty-elt, .bandeau-portail, .box, " +
                        ".messagebox, .thumb, .tright, .thumbinner").remove();
            }
        }

        // Try semantic HTML first
        if (mainElement == null) {
            mainElement = doc.select("article, main, [role=main]").first();
        }

        // Try common class patterns
        if (mainElement == null) {
            mainElement = doc.select(".post-content, .entry-content, .article-content, .article-body, .article-wrapper").first();
        }

        // Try finding the largest content block (heuristic for main content)
        if (mainElement == null) {
            Elements candidates = doc.select("div.text, div[class*='content'], div[class*='post'], div[class*='article']");
            int maxLength = 0;
            for (Element candidate : candidates) {
                int textLength = candidate.text().length();
                if (textLength > maxLength) {
                    maxLength = textLength;
                    mainElement = candidate;
                }
            }
        }

        // Fallback to body
        if (mainElement == null) {
            mainElement = doc.body();
        }

        // Remove metadata/footer elements from selected content
        mainElement.select(".article-meta, .author-info, .tag-container, " +
                ".improve, .article-tags, header").remove();

        return mainElement;
    }

    public void shutdown() {
        try {
            executorService.shutdown();
        } catch (Exception e) {
            logger.warn("Error shutting down ContentFetcherTool: {}", e.getMessage());
        }
    }
}