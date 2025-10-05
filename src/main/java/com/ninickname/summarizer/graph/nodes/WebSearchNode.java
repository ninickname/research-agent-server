package com.ninickname.summarizer.graph.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;
import com.ninickname.summarizer.model.SearxngResponse;
import com.ninickname.summarizer.tool.WebSearchTool;

/**
 * Node that performs web search using the optimized query.
 */
public class WebSearchNode extends BaseGraphNode {
    private final WebSearchTool webSearchTool;
    private final ObjectMapper objectMapper;

    public WebSearchNode(WebSearchTool webSearchTool) {
        super(NodeType.WEB_SEARCH);
        this.webSearchTool = webSearchTool;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected ResearchGraphState executeInternal(ResearchGraphState state) {
        String query = state.getOptimizedQuery();
        int resultCount = state.getResultCount();

        logger.info("Searching for '{}' with target count: {}", query, resultCount);

        SearxngResponse searchResults = webSearchTool.search(query, resultCount);

        logger.info("Found {} search results", searchResults.results().size());

        // Emit search results for SSE
        try {
            emitProgress(state, "search_results", objectMapper.writeValueAsString(searchResults));
        } catch (Exception e) {
            logger.warn("Failed to serialize search results: {}", e.getMessage());
        }

        return state.toBuilder()
                .searchResults(searchResults)
                .build();
    }
}
