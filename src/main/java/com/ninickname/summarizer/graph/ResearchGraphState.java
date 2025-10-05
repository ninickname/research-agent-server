package com.ninickname.summarizer.graph;

import com.ninickname.summarizer.model.ContentData;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.model.SearxngResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable state object that flows through the research graph.
 *
 * Best Practices:
 * - Immutable: Each node returns a new state (functional programming style)
 * - Complete: Contains all data needed by any node
 * - Metadata: Tracks timing, errors, and progress
 * - SSE Support: Optional emitter for streaming updates
 */
public class ResearchGraphState {
    // Input parameters
    private final String topic;
    private final int resultCount;
    private final boolean skipContentFetch;

    // Research data (populated by nodes)
    private final String optimizedQuery;
    private final SearxngResponse searchResults;
    private final String quickSummary;
    private final List<ContentData> structuredContents;
    private final String comprehensiveSummary;

    // Metadata
    private final Map<NodeType, Long> nodeDurations; // Track performance per node
    private final Map<NodeType, String> nodeErrors;  // Track errors per node
    private final long startTime;
    private final NodeType currentNode;

    // SSE support (optional)
    private final SseEmitter emitter;

    // Private constructor - use builder
    private ResearchGraphState(Builder builder) {
        this.topic = builder.topic;
        this.resultCount = builder.resultCount;
        this.skipContentFetch = builder.skipContentFetch;
        this.optimizedQuery = builder.optimizedQuery;
        this.searchResults = builder.searchResults;
        this.quickSummary = builder.quickSummary;
        this.structuredContents = builder.structuredContents;
        this.comprehensiveSummary = builder.comprehensiveSummary;
        this.nodeDurations = new HashMap<>(builder.nodeDurations);
        this.nodeErrors = new HashMap<>(builder.nodeErrors);
        this.startTime = builder.startTime;
        this.currentNode = builder.currentNode;
        this.emitter = builder.emitter;
    }

    // Getters
    public String getTopic() {
        return topic;
    }

    public int getResultCount() {
        return resultCount;
    }

    public boolean isSkipContentFetch() {
        return skipContentFetch;
    }

    public String getOptimizedQuery() {
        return optimizedQuery;
    }

    public SearxngResponse getSearchResults() {
        return searchResults;
    }

    public String getQuickSummary() {
        return quickSummary;
    }

    public List<ContentData> getStructuredContents() {
        return structuredContents;
    }

    public String getComprehensiveSummary() {
        return comprehensiveSummary;
    }

    public Map<NodeType, Long> getNodeDurations() {
        return new HashMap<>(nodeDurations);
    }

    public Map<NodeType, String> getNodeErrors() {
        return new HashMap<>(nodeErrors);
    }

    public long getStartTime() {
        return startTime;
    }

    public NodeType getCurrentNode() {
        return currentNode;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    /**
     * Get total elapsed time since graph started
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Check if a node has completed successfully
     */
    public boolean hasCompletedNode(NodeType nodeType) {
        return nodeDurations.containsKey(nodeType) && !nodeErrors.containsKey(nodeType);
    }

    /**
     * Convert to ResearchResult for API response
     */
    public ResearchResult toResearchResult() {
        ResearchResult result = new ResearchResult(topic);
        result.setOptimizedQuery(optimizedQuery);
        result.setSearchResults(searchResults);
        result.setQuickSummary(quickSummary);
        result.setStructuredContents(structuredContents != null ? structuredContents : new ArrayList<>());
        result.setComprehensiveSummary(comprehensiveSummary);
        return result;
    }

    /**
     * Create a new state with updated values (immutable pattern)
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Create initial state from user request
     */
    public static Builder builder(String topic, int resultCount, boolean skipContentFetch) {
        return new Builder(topic, resultCount, skipContentFetch);
    }

    /**
     * Builder pattern for creating and updating state
     */
    public static class Builder {
        private String topic;
        private int resultCount;
        private boolean skipContentFetch;
        private String optimizedQuery;
        private SearxngResponse searchResults;
        private String quickSummary;
        private List<ContentData> structuredContents;
        private String comprehensiveSummary;
        private Map<NodeType, Long> nodeDurations = new HashMap<>();
        private Map<NodeType, String> nodeErrors = new HashMap<>();
        private long startTime;
        private NodeType currentNode;
        private SseEmitter emitter;

        // Initial builder
        public Builder(String topic, int resultCount, boolean skipContentFetch) {
            this.topic = topic;
            this.resultCount = resultCount;
            this.skipContentFetch = skipContentFetch;
            this.startTime = System.currentTimeMillis();
            this.currentNode = NodeType.OPTIMIZE_QUERY;
        }

        // Copy builder (for immutable updates)
        public Builder(ResearchGraphState state) {
            this.topic = state.topic;
            this.resultCount = state.resultCount;
            this.skipContentFetch = state.skipContentFetch;
            this.optimizedQuery = state.optimizedQuery;
            this.searchResults = state.searchResults;
            this.quickSummary = state.quickSummary;
            this.structuredContents = state.structuredContents;
            this.comprehensiveSummary = state.comprehensiveSummary;
            this.nodeDurations = new HashMap<>(state.nodeDurations);
            this.nodeErrors = new HashMap<>(state.nodeErrors);
            this.startTime = state.startTime;
            this.currentNode = state.currentNode;
            this.emitter = state.emitter;
        }

        public Builder optimizedQuery(String optimizedQuery) {
            this.optimizedQuery = optimizedQuery;
            return this;
        }

        public Builder searchResults(SearxngResponse searchResults) {
            this.searchResults = searchResults;
            return this;
        }

        public Builder quickSummary(String quickSummary) {
            this.quickSummary = quickSummary;
            return this;
        }

        public Builder structuredContents(List<ContentData> structuredContents) {
            this.structuredContents = structuredContents;
            return this;
        }

        public Builder comprehensiveSummary(String comprehensiveSummary) {
            this.comprehensiveSummary = comprehensiveSummary;
            return this;
        }

        public Builder recordNodeDuration(NodeType node, long durationMs) {
            this.nodeDurations.put(node, durationMs);
            return this;
        }

        public Builder recordNodeError(NodeType node, String error) {
            this.nodeErrors.put(node, error);
            return this;
        }

        public Builder currentNode(NodeType currentNode) {
            this.currentNode = currentNode;
            return this;
        }

        public Builder emitter(SseEmitter emitter) {
            this.emitter = emitter;
            return this;
        }

        public ResearchGraphState build() {
            return new ResearchGraphState(this);
        }
    }
}
