package com.ninickname.summarizer.graph;

import com.ninickname.summarizer.agents.QueryOptimizerAgent;
import com.ninickname.summarizer.agents.QuickSummaryAgent;
import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.tool.ContentFetcherTool;
import com.ninickname.summarizer.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spring service that executes the research graph.
 *
 * Best Practices:
 * - Spring Integration: Managed as a @Service bean
 * - Lazy Graph Building: Graph built once and reused
 * - Thread Safety: Immutable state ensures thread-safe execution
 * - SSE Support: Optional streaming for progress updates
 */
@Service
public class ResearchGraphService {
    private static final Logger logger = LoggerFactory.getLogger(ResearchGraphService.class);

    private final ResearchGraph researchGraph;
    private final ExecutorService executorService;

    public ResearchGraphService(QueryOptimizerAgent queryOptimizerAgent,
                                WebSearchTool webSearchTool,
                                ContentFetcherTool contentFetcherTool,
                                QuickSummaryAgent quickSummaryAgent,
                                SummarizingAgent summarizingAgent) {
        this.executorService = Executors.newCachedThreadPool();

        // Build the graph once on service initialization
        ResearchGraphBuilder builder = new ResearchGraphBuilder(
                queryOptimizerAgent,
                webSearchTool,
                contentFetcherTool,
                quickSummaryAgent,
                summarizingAgent,
                executorService
        );

        this.researchGraph = builder.buildDefaultGraph();
        logger.info("ResearchGraphService initialized with graph: {}", researchGraph);
    }

    /**
     * Execute research using the graph (synchronous)
     */
    public ResearchResult research(String topic, int resultCount, boolean skipContentFetch) {
        logger.info("Starting graph-based research for topic: '{}' (count: {}, skipContentFetch: {})",
                topic, resultCount, skipContentFetch);

        // Create initial state
        ResearchGraphState initialState = ResearchGraphState
                .builder(topic, resultCount, skipContentFetch)
                .build();

        // Execute the graph
        ResearchGraphState finalState = researchGraph.execute(initialState);

        // Convert to ResearchResult
        return finalState.toResearchResult();
    }

    /**
     * Execute research with SSE streaming (asynchronous)
     */
    public SseEmitter researchWithProgress(String topic, int resultCount, boolean skipContentFetch) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        logger.info("Starting streaming graph-based research for topic: '{}' (count: {}, skipContentFetch: {})",
                topic, resultCount, skipContentFetch);

        executorService.execute(() -> {
            try {
                // Create initial state with emitter
                ResearchGraphState initialState = ResearchGraphState
                        .builder(topic, resultCount, skipContentFetch)
                        .emitter(emitter)
                        .build();

                // Execute the graph
                ResearchGraphState finalState = researchGraph.execute(initialState);

                // Send completion event
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("true"));

                emitter.complete();
                logger.info("Streaming research completed successfully");

            } catch (Exception e) {
                logger.error("Streaming research failed: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * Get the graph for inspection/debugging
     */
    public ResearchGraph getGraph() {
        return researchGraph;
    }
}
