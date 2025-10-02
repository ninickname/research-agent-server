package com.ninickname.summarizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.agents.QueryOptimizerAgent;
import com.ninickname.summarizer.agents.QuickSummaryAgent;
import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.model.SearxngResponse;
import com.ninickname.summarizer.model.SearxngResult;
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

    public ResearchResult research(String topic, int resultCount) {
        logger.info("Starting research for topic: {} with {} results", topic, resultCount);

        // Initialize result object
        ResearchResult result = new ResearchResult(topic);

        try {
            // Step 1: Optimize the search query
            logger.info("Step 1: Optimizing search query using QueryOptimizerAgent...");
            String optimizedQuery = queryOptimizerAgent.optimizeQuery(topic);
            logger.info("Original query: '{}' -> Optimized query: '{}'", topic, optimizedQuery);

            result.setOptimizedQuery(optimizedQuery);

            // Step 2: Web search using WebSearchTool directly
            logger.info("Step 2: Searching for topic using WebSearchTool with {} results...", resultCount);
            SearxngResponse searchResults = webSearchTool.search(optimizedQuery, resultCount);
            logger.info("WebSearchTool found {} search results", searchResults.results().size());

            result.setSearchResults(searchResults);

            // Step 3: Extract snippets and start quick summary (parallel)
            List<String> snippets = searchResults.results().stream()
                    .map(SearxngResult::content)
                    .filter(content -> content != null && !content.trim().isEmpty())
                    .toList();

            // Start quick summary async
            CompletableFuture<Void> quickSummaryFuture = CompletableFuture.runAsync(() -> {
                logger.info("Step 3: Generating quick summary from {} snippets...", snippets.size());
                String prompt = QuickSummaryAgent.buildPrompt(topic, snippets);
                String quickSummary = quickSummaryAgent.summarizeSnippets(prompt);
                logger.info("Quick summary completed: {}", quickSummary.substring(0, Math.min(100, quickSummary.length())));
                result.setQuickSummary(quickSummary);
            });

            // Step 4: Fetch full content from URLs using ContentFetcherTool (has built-in parallel execution)
            List<String> urls = searchResults.results().stream()
                    .map(SearxngResult::url)
                    .toList();

            logger.info("Step 4: Fetching content from {} URLs using ContentFetcherTool with parallel execution...", urls.size());
            List<String> contents = contentFetcherTool.fetchMultipleContents(urls);
            logger.info("ContentFetcherTool fetched full content from {} sources", contents.size());

            result.setContents(contents);

            // Step 5: Final comprehensive summary
            if (!contents.isEmpty()) {
                logger.info("Step 5: Sending {} sources to SummarizingAgent...", contents.size());
                String comprehensiveSummary = summarizingAgent.summarizeResearch(topic, contents);
                logger.info("SummarizingAgent completed summarization");
                result.setComprehensiveSummary(comprehensiveSummary);
            } else {
                logger.warn("Step 5: No content fetched from URLs, using quick summary from snippets only");
                result.setComprehensiveSummary("Unable to fetch full content from sources. " +
                        "Quick summary based on search snippets: " + result.getQuickSummary());
            }

            // Wait for quick summary to complete (if not already)
            quickSummaryFuture.join();

            logger.info("Research completed with both quick and comprehensive summaries");
            return result;
        } catch (Exception e) {
            logger.error("Research failed for topic '{}': {}", topic, e.getMessage(), e);
            throw new RuntimeException("Research failed for topic '" + topic + "': " + e.getMessage(), e);
        }
    }

    public SseEmitter researchWithProgress(String topic, int resultCount) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        logger.info("Starting streaming research for topic: {} with {} results", topic, resultCount);

        executorService.execute(() -> {
            try {
                ResearchResult result = new ResearchResult(topic);

                // Step 1: Optimize query
                logger.info("Step 1: Optimizing search query...");
                emitProgress(emitter, "step", "optimizing_query");
                String optimizedQuery = queryOptimizerAgent.optimizeQuery(topic);
                result.setOptimizedQuery(optimizedQuery);
                emitProgress(emitter, "optimized_query", optimizedQuery);

                // Step 2: Web search
                logger.info("Step 2: Searching web...");
                emitProgress(emitter, "step", "searching");
                SearxngResponse searchResults = webSearchTool.search(optimizedQuery, resultCount);
                result.setSearchResults(searchResults);
                emitProgress(emitter, "search_results", objectMapper.writeValueAsString(searchResults));

                // Step 3: Quick summary (async)
                List<String> snippets = searchResults.results().stream()
                        .map(SearxngResult::content)
                        .filter(content -> content != null && !content.trim().isEmpty())
                        .toList();

                CompletableFuture<Void> quickSummaryFuture = CompletableFuture.runAsync(() -> {
                    logger.info("Step 3: Generating quick summary...");
                    emitProgress(emitter, "step", "quick_summary");
                    String prompt = QuickSummaryAgent.buildPrompt(topic, snippets);
                    String quickSummary = quickSummaryAgent.summarizeSnippets(prompt);
                    result.setQuickSummary(quickSummary);
                    emitProgress(emitter, "quick_summary", quickSummary);
                });

                // Step 4: Fetch content
                List<String> urls = searchResults.results().stream()
                        .map(SearxngResult::url)
                        .toList();

                logger.info("Step 4: Fetching content...");
                emitProgress(emitter, "step", "fetching_content");
                List<String> contents = contentFetcherTool.fetchMultipleContents(urls);
                result.setContents(contents);
                emitProgress(emitter, "contents_array", objectMapper.writeValueAsString(contents));

                // Step 5: Comprehensive summary
                if (!contents.isEmpty()) {
                    logger.info("Step 5: Creating comprehensive summary...");
                    emitProgress(emitter, "step", "comprehensive_summary");
                    String comprehensiveSummary = summarizingAgent.summarizeResearch(topic, contents);
                    result.setComprehensiveSummary(comprehensiveSummary);
                    emitProgress(emitter, "comprehensive_summary", comprehensiveSummary);
                } else {
                    logger.warn("No content fetched, using quick summary only");
                    result.setComprehensiveSummary("Unable to fetch full content. Quick summary: " + result.getQuickSummary());
                    emitProgress(emitter, "comprehensive_summary", result.getComprehensiveSummary());
                }

                // Wait for quick summary
                quickSummaryFuture.join();

                // Send complete event
                emitProgress(emitter, "complete", "true");
                emitter.complete();

                logger.info("Streaming research completed");
            } catch (Exception e) {
                logger.error("Streaming research failed: {}", e.getMessage(), e);
                emitProgress(emitter, "error", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void emitProgress(SseEmitter emitter, String eventType, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (Exception e) {
            logger.error("Failed to emit progress event: {}", e.getMessage());
        }
    }
}