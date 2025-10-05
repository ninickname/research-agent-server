package com.ninickname.summarizer.tool;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.model.SearxngResponse;
import com.ninickname.summarizer.model.SearxngResult;
import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WebSearchTool {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchTool.class);
    private static final int MAX_PAGES = 25; // Maximum pagination attempts (to reach 100+ results)
    private static final int MAX_TOTAL_RESULTS = 100; // Hard limit on total results
    private static final int CONSECUTIVE_EMPTY_THRESHOLD = 5; // Stop after 5 consecutive empty pages

    // File extensions to exclude (we only want HTML pages)
    private static final Set<String> EXCLUDED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".zip", ".rar", ".tar", ".gz", ".7z",
            ".mp3", ".mp4", ".avi", ".mov", ".wmv",
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".exe", ".dmg", ".iso"
    );

    private final ObjectMapper objectMapper;
    private final String mcpUrl;

    public WebSearchTool(@Value("${mcp.web.url:http://localhost:9101}") String mcpUrl) {
        this.mcpUrl = mcpUrl;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        logger.info("WebSearchTool initialized with MCP URL: {}", mcpUrl);
    }

    @Tool("Search the web for information about a given topic with pagination support (up to 100 results)")
    public SearxngResponse search(String query, int userRequestedCount) {
        // Cap user request at maximum
        int targetResults = Math.min(userRequestedCount, MAX_TOTAL_RESULTS);
        logger.info("Searching for: '{}' (user wants {} sources, max: {})", query, targetResults, MAX_TOTAL_RESULTS);

        List<SearxngResult> allResults = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>(); // Deduplicate by URL
        int currentPage = 1;
        int consecutiveEmptyPages = 0; // Track consecutive pages with no new results

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(mcpUrl).build();
        try (McpSyncClient client = McpClient
                .sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build()) {

            client.initialize();

            // Keep fetching pages until we have enough results for user's request
            while (allResults.size() < targetResults && currentPage <= MAX_PAGES) {
                logger.info("Fetching page {} (current: {} valid results, target: {})", currentPage, allResults.size(), targetResults);

                try {
                    // Make MCP call with page number
                    McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder()
                            .name("web_search")
                            .arguments(Map.of(
                                    "query", query,
                                    "page", currentPage


                            ))
                            .build();

                    McpSchema.CallToolResult responseFromTool = client.callTool(toolRequest);
                    String textualResponse = ((McpSchema.TextContent) responseFromTool.content().get(0)).text();

                    SearxngResponse pageResponse = objectMapper.readValue(textualResponse, SearxngResponse.class);
                    int pageResultCount = pageResponse.results().size();

                    logger.info("Page {} returned {} raw results", currentPage, pageResultCount);

                    // If page returned no results, stop pagination
                    if (pageResultCount == 0) {
                        logger.info("Page {} returned 0 results, stopping pagination", currentPage);
                        break;
                    }

                    // Filter and add unique, valid results
                    int addedCount = 0;
                    int skippedFiles = 0;
                    int duplicates = 0;

                    for (SearxngResult result : pageResponse.results()) {
                        // Skip if no URL
                        if (result.url() == null || result.url().trim().isEmpty()) {
                            continue;
                        }

                        // Skip duplicate URLs
                        if (seenUrls.contains(result.url())) {
                            duplicates++;
                            continue;
                        }

                        // Skip file URLs (PDFs, docs, etc.)
                        if (isFileUrl(result.url())) {
                            skippedFiles++;
                            logger.debug("Skipping file URL: {}", result.url());
                            continue;
                        }

                        // Add valid result
                        seenUrls.add(result.url());
                        allResults.add(result);
                        addedCount++;
                    }

                    logger.info("Page {}: added {} valid results (skipped {} files, {} duplicates) - total: {}",
                            currentPage, addedCount, skippedFiles, duplicates, allResults.size());

                    // Track consecutive empty pages
                    if (addedCount == 0) {
                        consecutiveEmptyPages++;
                        logger.info("Page {} added 0 valid results (consecutive empty: {})", currentPage, consecutiveEmptyPages);
                        // Only stop if we've had several consecutive pages with no new results
                        if (consecutiveEmptyPages >= CONSECUTIVE_EMPTY_THRESHOLD) {
                            logger.info("Stopping pagination after {} consecutive pages with no new results", consecutiveEmptyPages);
                            break;
                        }
                    } else {
                        consecutiveEmptyPages = 0; // Reset counter when we get results
                    }

                    currentPage++;

                } catch (Exception e) {
                    logger.warn("Failed to fetch page {}: {}, stopping pagination", currentPage, e.getMessage());
                    break;
                }
            }

            logger.info("Search completed: {} valid web pages from {} pages (requested: {})",
                    allResults.size(), currentPage - 1, targetResults);

            if (allResults.size() < targetResults) {
                logger.warn("Only retrieved {} results out of {} requested. Pagination may have stopped early.",
                        allResults.size(), targetResults);
            }

            return new SearxngResponse(query, allResults, List.of());

        } catch (Exception e) {
            logger.error("Web search failed for query '{}': {}", query, e.getMessage(), e);
            throw new RuntimeException("Web search failed for query '" + query + "': " + e.getMessage(), e);
        }
    }

    /**
     * Check if URL points to a file (PDF, doc, etc.) instead of a web page
     */
    private boolean isFileUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return EXCLUDED_EXTENSIONS.stream().anyMatch(lowerUrl::endsWith);
    }
}




