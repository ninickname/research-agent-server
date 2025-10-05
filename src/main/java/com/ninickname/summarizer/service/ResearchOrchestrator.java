package com.ninickname.summarizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.agents.QueryOptimizerAgent;
import com.ninickname.summarizer.agents.QuickSummaryAgent;
import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.model.SearxngResponse;
import com.ninickname.summarizer.model.SearxngResult;
import com.ninickname.summarizer.model.ContentData;
import com.ninickname.summarizer.tool.ContentFetcherTool;
import com.ninickname.summarizer.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ResearchOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ResearchOrchestrator.class);
    private final QueryOptimizerAgent queryOptimizerAgent;
    private final WebSearchTool webSearchTool;
    private final ContentFetcherTool contentFetcherTool;
    private final QuickSummaryAgent quickSummaryAgent;
    private final SummarizingAgent summarizingAgent;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public ResearchOrchestrator(QueryOptimizerAgent queryOptimizerAgent,
                                WebSearchTool webSearchTool,
                                ContentFetcherTool contentFetcherTool,
                                QuickSummaryAgent quickSummaryAgent,
                                SummarizingAgent summarizingAgent) {
        this.queryOptimizerAgent = queryOptimizerAgent;
        this.webSearchTool = webSearchTool;
        this.contentFetcherTool = contentFetcherTool;
        this.quickSummaryAgent = quickSummaryAgent;
        this.summarizingAgent = summarizingAgent;
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newCachedThreadPool();
    }

    public ResearchResult research(String topic, int resultCount, boolean skipContentFetch) {
        logger.info("Starting synchronous research for topic: {} with {} results (skipContentFetch: {})",
                topic, resultCount, skipContentFetch);

        // Perform the research using the core implementation
        return performResearch(topic, resultCount, skipContentFetch, null);
    }

    public SseEmitter researchWithProgress(String topic, int resultCount, boolean skipContentFetch) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        logger.info("Starting streaming research for topic: {} with {} results (skipContentFetch: {})",
                topic, resultCount, skipContentFetch);

        executorService.execute(() -> {
            try {
                ResearchResult result = performResearch(topic, resultCount, skipContentFetch, emitter);

                // Send complete event
                emitProgress(emitter, "complete", "true");
                emitter.complete();

            } catch (Exception e) {
                logger.error("Streaming research failed: {}", e.getMessage(), e);
                emitProgress(emitter, "error", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private ResearchResult performResearch(String topic, int resultCount, boolean skipContentFetch, SseEmitter emitter) {
        long totalStartTime = System.currentTimeMillis();
        ResearchResult result = new ResearchResult(topic);

        try {
            // Step 1: Optimize query
            long step1Duration = optimizeQuery(topic, result, emitter);

            // Step 2: Web search
            long step2Duration = performWebSearch(result.getOptimizedQuery(), resultCount, result, emitter);

            // Step 3: Quick summary (async - runs in background, no waiting)
            startQuickSummary(topic, result, emitter);

            if (skipContentFetch) {
                // Skip content fetching and comprehensive summary - stop after quick summary
                logger.info("Skipping content fetch and comprehensive summary (skipContentFetch=true)");
                emitProgress(emitter, "step", "skipped_content_fetch");

                long totalDuration = System.currentTimeMillis() - totalStartTime;
                logger.info("=== RESEARCH COMPLETED (QUICK MODE) ===");
                logger.info("Total time: {}ms ({} seconds) [excluding async quick summary]",
                        totalDuration, totalDuration / 1000.0);
                logger.info("Breakdown: Step1={}ms, Step2={}ms, Step3=async, Step4=skipped, Step5=skipped",
                        step1Duration, step2Duration);
                logger.info("========================");

                return result;
            }

            // Step 4: Fetch structured content
            long step4Duration = fetchStructuredContent(result.getSearchResults(), resultCount, result, emitter);

            // Step 5: Comprehensive summary
            long step5Duration = createComprehensiveSummary(topic, result, emitter);

            // Total timing (excluding async quick summary)
            long totalDuration = System.currentTimeMillis() - totalStartTime;
            logger.info("=== RESEARCH COMPLETED ===");
            logger.info("Total time: {}ms ({} seconds) [excluding async quick summary]",
                    totalDuration, totalDuration / 1000.0);
            logger.info("Breakdown: Step1={}ms, Step2={}ms, Step3=async, Step4={}ms, Step5={}ms",
                    step1Duration, step2Duration, step4Duration, step5Duration);
            logger.info("========================");

            return result;

        } catch (Exception e) {
            logger.error("Research failed for topic '{}': {}", topic, e.getMessage(), e);
            throw new RuntimeException("Research failed for topic '" + topic + "': " + e.getMessage(), e);
        }
    }

    private long optimizeQuery(String topic, ResearchResult result, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        logger.info("Step 1: Optimizing search query...");
        emitProgress(emitter, "step", "optimizing_query");

        String optimizedQuery = queryOptimizerAgent.optimizeQuery(topic);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Step 1 completed in {}ms - Original: '{}' -> Optimized: '{}'",
                duration, topic, optimizedQuery);
        result.setOptimizedQuery(optimizedQuery);
        emitProgress(emitter, "optimized_query", optimizedQuery);

        return duration;
    }

    private long performWebSearch(String query, int resultCount, ResearchResult result, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        logger.info("Step 2: Searching web...");
        emitProgress(emitter, "step", "searching");

        // Pass user's request - will paginate MCP only if needed
        SearxngResponse searchResults = webSearchTool.search(query, resultCount);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("Step 2 completed in {}ms - Found {} results (will fetch content for: {})",
                duration, searchResults.results().size(), resultCount);
        result.setSearchResults(searchResults);

        try {
            emitProgress(emitter, "search_results", objectMapper.writeValueAsString(searchResults));
        } catch (Exception e) {
            logger.warn("Failed to serialize search results for progress: {}", e.getMessage());
        }

        return duration;
    }

    private void startQuickSummary(String topic, ResearchResult result, SseEmitter emitter) {
        List<String> snippets = result.getSearchResults().results().stream()
                .map(SearxngResult::content)
                .filter(content -> content != null && !content.trim().isEmpty())
                .toList();

        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            logger.info("Step 3: Generating quick summary from {} snippets...", snippets.size());
            emitProgress(emitter, "step", "quick_summary");

            String prompt = QuickSummaryAgent.buildPrompt(topic, snippets);
            String quickSummary = quickSummaryAgent.summarizeSnippets(prompt);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Step 3 (async) completed in {}ms - Quick summary generated", duration);
            result.setQuickSummary(quickSummary);
            emitProgress(emitter, "quick_summary", quickSummary);
        });
    }

    private long fetchStructuredContent(SearxngResponse searchResults, int resultCount,
                                       ResearchResult result, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();

        // Create a map from URL to search result for metadata lookup
        var urlToSearchResult = searchResults.results().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SearxngResult::url,
                        r -> r,
                        (a, b) -> a // In case of duplicates, keep first
                ));

        // Try to fetch content until we have enough successful fetches
        // Start with more URLs than needed since some may fail
        List<String> allUrls = searchResults.results().stream()
                .map(SearxngResult::url)
                .toList();

        logger.info("Step 4: Fetching structured content (target: {} sources, available: {} URLs)",
                resultCount, allUrls.size());
        emitProgress(emitter, "step", "fetching_content");

        // Fetch from URLs in batches until we have enough content
        List<ContentData> structuredContents = new java.util.ArrayList<>();
        int batchSize = Math.min(resultCount * 2, allUrls.size()); // Fetch 2x what we need initially
        int urlIndex = 0;

        while (structuredContents.size() < resultCount && urlIndex < allUrls.size()) {
            int endIndex = Math.min(urlIndex + batchSize, allUrls.size());
            List<String> urlBatch = allUrls.subList(urlIndex, endIndex);

            logger.info("Fetching batch of {} URLs (current: {}/{}, index: {}-{})",
                    urlBatch.size(), structuredContents.size(), resultCount, urlIndex, endIndex);

            var batchResults = contentFetcherTool.fetchMultipleStructuredContents(urlBatch);

            // Populate engine and score from search results, filter out empty/low-quality content
            for (ContentData content : batchResults) {
                // Skip null, empty, or low-quality content (< 150 characters)
                if (content == null || content.totalCharacters() < 150) {
                    logger.warn("Skipping low-quality content ({} chars) from URL: {}",
                            content != null ? content.totalCharacters() : 0,
                            content != null ? content.url() : "unknown");
                    continue;
                }

                SearxngResult searchResult = urlToSearchResult.get(content.url());
                if (searchResult != null) {
                    // Create new ContentData with engine and score populated
                    ContentData enrichedContent = new ContentData(
                            content.url(),
                            content.title(),
                            content.mainHeading(),
                            content.sections(),
                            content.totalCharacters(),
                            content.hasStructure(),
                            searchResult.engine(),
                            searchResult.score()
                    );
                    structuredContents.add(enrichedContent);
                } else {
                    structuredContents.add(content);
                }
            }

            urlIndex = endIndex;

            // If we have enough, break
            if (structuredContents.size() >= resultCount) {
                break;
            }

            // If we still need more and have more URLs, continue with smaller batches
            batchSize = resultCount - structuredContents.size() + 2; // +2 buffer for failures
        }

        // Limit to requested count
        if (structuredContents.size() > resultCount) {
            structuredContents = structuredContents.subList(0, resultCount);
        }

        long duration = System.currentTimeMillis() - startTime;

        logger.info("Step 4 completed in {}ms - Fetched {} structured contents (target: {}, avg: {}ms per source)",
                duration, structuredContents.size(), resultCount,
                structuredContents.isEmpty() ? 0 : duration / structuredContents.size());

        result.setStructuredContents(structuredContents);

        try {
            emitProgress(emitter, "structured_contents", objectMapper.writeValueAsString(structuredContents));
        } catch (Exception e) {
            logger.warn("Failed to serialize structured contents for progress: {}", e.getMessage());
        }

        return duration;
    }

    private long createComprehensiveSummary(String topic, ResearchResult result, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        List<ContentData> structuredContents = result.getStructuredContents();

        if (!structuredContents.isEmpty()) {
            logger.info("Step 5: Creating comprehensive summary from {} sources...", structuredContents.size());
            emitProgress(emitter, "step", "comprehensive_summary");

            // Convert to formatted strings for the summarizing agent
            List<String> formattedContents = structuredContents.stream()
                    .map(com.ninickname.summarizer.formatter.StructuredContentFormatter::toFormattedString)
                    .toList();

            // Extract source URLs for citation
            List<String> sourceUrls = structuredContents.stream()
                    .map(ContentData::url)
                    .toList();

            String comprehensiveSummary = summarizingAgent.summarizeResearch(topic, formattedContents, sourceUrls);
            long duration = System.currentTimeMillis() - startTime;

            logger.info("Step 5 completed in {}ms - Comprehensive summary generated", duration);
            result.setComprehensiveSummary(comprehensiveSummary);
            emitProgress(emitter, "comprehensive_summary", comprehensiveSummary);

            return duration;
        } else {
            logger.warn("Step 5: No content fetched from URLs, using quick summary from snippets only");
            result.setComprehensiveSummary("Unable to fetch full content. Quick summary: " + result.getQuickSummary());
            emitProgress(emitter, "comprehensive_summary", result.getComprehensiveSummary());
            return 0;
        }
    }

    private void emitProgress(SseEmitter emitter, String eventType, String data) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (Exception e) {
            logger.error("Failed to emit progress event: {}", e.getMessage());
        }
    }
}