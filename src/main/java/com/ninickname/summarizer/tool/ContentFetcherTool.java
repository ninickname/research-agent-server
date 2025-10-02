package com.ninickname.summarizer.tool;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ContentFetcherTool {
    private static final Logger logger = LoggerFactory.getLogger(ContentFetcherTool.class);
    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ExecutorService executorService;

    public ContentFetcherTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.executorService = Executors.newFixedThreadPool(10);
    }

    public ContentFetcherTool(int threadPoolSize) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Tool("Fetch content from a single URL")
    public String fetchSingleContent(String url) {
        logger.info("ContentFetcherTool: Fetching content from URL: {}", url);
        try {
            String content = fetchContent(url);
            if (content != null && !content.trim().isEmpty()) {
                logger.info("ContentFetcherTool: Successfully fetched content from {}", url);
                return content;
            }
            logger.warn("ContentFetcherTool: No content retrieved from {}", url);
            return null;
        } catch (Exception e) {
            logger.error("ContentFetcherTool: Failed to fetch content from {}: {}", url, e.getMessage());
            return null;
        }
    }

    @Tool("Fetch content from URLs in parallel")
    public List<String> fetchMultipleContents(List<String> urls) {
        logger.info("ContentFetcherTool: Fetching content from {} URLs", urls.size());
        try {
            List<CompletableFuture<String>> futures = urls.stream()
                    .map(this::fetchContentAsync)
                    .toList();

            List<String> contents = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .toList();

            logger.info("ContentFetcherTool: Successfully fetched {} contents", contents.size());
            return contents;
        } catch (Exception e) {
            logger.error("Failed to fetch content from {} URLs: {}", urls.size(), e.getMessage(), e);
            throw new RuntimeException("Content fetching failed for " + urls.size() + " URLs: " + e.getMessage(), e);
        }
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

    private String fetchContent(String url) throws IOException, InterruptedException {
        logger.debug("Fetching content from: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Research Agent) Java HttpClient")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
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

            return extractTextFromHtml(html, url);
        } catch (Exception e) {
            logger.warn("Failed to fetch {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String extractTextFromHtml(String html, String url) {
        try {
            Document doc = Jsoup.parse(html);

            // Remove noise elements (headers, footers, navigation, ads, etc.)
            doc.select("script, style, nav, footer, header, aside, " +
                    ".header, .footer, .navigation, .nav, .menu, " +
                    ".sidebar, .advertisement, .ad, .ads, " +
                    ".social-share, .share, .comments, " +
                    ".cookie-banner, .cookie-notice, " +
                    ".popup, .modal, .overlay").remove();

            // Try to find main content area (prioritize article, main, or divs with content class)
            String text;
            var mainContent = doc.select("article, main, [role=main], .content, .post, .article-body").first();

            if (mainContent != null) {
                text = mainContent.text();
                logger.debug("Extracted text from main content area for {}", url);
            } else {
                // Fallback to body if no main content found
                text = doc.body().text();
                logger.debug("No main content area found, using full body for {}", url);
            }

            // Remove common header/footer junk patterns from beginning and end
            text = stripJunkFromEnds(text);

            // Limit content length and clean up
            if (text.length() > MAX_CONTENT_LENGTH) {
                text = text.substring(0, MAX_CONTENT_LENGTH) + "...";
            }

            // Clean up whitespace
            text = text.replaceAll("\\s+", " ").trim();

            logger.debug("Extracted {} characters from {}", text.length(), url);
            return text;

        } catch (Exception e) {
            logger.warn("Failed to parse HTML from {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String stripJunkFromEnds(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Common junk patterns at the beginning
        String[] startPatterns = {
                "^(Skip to|Jump to|Go to) (main )?content\\s*",
                "^Menu\\s+",
                "^Navigation\\s+",
                "^Home\\s+About\\s+Contact\\s*",
                "^Cookie (Notice|Policy|Settings|Consent)\\s+.*?Accept.*?\\s+",
                "^Sign in\\s+Register\\s+",
                "^Subscribe\\s+.*?\\s+"
        };

        for (String pattern : startPatterns) {
            text = text.replaceFirst("(?i)" + pattern, "");
        }

        // Common junk patterns at the end
        String[] endPatterns = {
                "\\s+(Copyright|©)\\s+\\d{4}.*$",
                "\\s+All [Rr]ights [Rr]eserved.*$",
                "\\s+Privacy Policy.*$",
                "\\s+Terms (of Service|and Conditions).*$",
                "\\s+Follow us on.*$",
                "\\s+Share this.*$",
                "\\s+Subscribe to.*$",
                "\\s+Sign up for.*$"
        };

        for (String pattern : endPatterns) {
            text = text.replaceFirst("(?i)" + pattern, "");
        }

        return text.trim();
    }

    public void shutdown() {
        try {
            executorService.shutdown();
        } catch (Exception e) {
            logger.warn("Error shutting down ContentFetcherTool: {}", e.getMessage());
        }
    }
}