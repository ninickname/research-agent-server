package com.ninickname.summarizer.graph;

import com.ninickname.summarizer.agents.QueryOptimizerAgent;
import com.ninickname.summarizer.agents.QuickSummaryAgent;
import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.graph.nodes.*;
import com.ninickname.summarizer.tool.ContentFetcherTool;
import com.ninickname.summarizer.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Builder for constructing the research graph.
 *
 * Best Practices:
 * - Fluent API: Chainable methods for readability
 * - Separation of Concerns: Graph structure separate from node logic
 * - Validation: Ensures graph is valid before building
 * - Flexibility: Easy to add/remove nodes and edges
 */
public class ResearchGraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ResearchGraphBuilder.class);

    private final Map<NodeType, GraphNode> nodes = new HashMap<>();
    private final Map<NodeType, List<NodeType>> edges = new HashMap<>();
    private final ExecutorService executorService;

    // Dependencies
    private final QueryOptimizerAgent queryOptimizerAgent;
    private final WebSearchTool webSearchTool;
    private final ContentFetcherTool contentFetcherTool;
    private final QuickSummaryAgent quickSummaryAgent;
    private final SummarizingAgent summarizingAgent;

    public ResearchGraphBuilder(QueryOptimizerAgent queryOptimizerAgent,
                                WebSearchTool webSearchTool,
                                ContentFetcherTool contentFetcherTool,
                                QuickSummaryAgent quickSummaryAgent,
                                SummarizingAgent summarizingAgent,
                                ExecutorService executorService) {
        this.queryOptimizerAgent = queryOptimizerAgent;
        this.webSearchTool = webSearchTool;
        this.contentFetcherTool = contentFetcherTool;
        this.quickSummaryAgent = quickSummaryAgent;
        this.summarizingAgent = summarizingAgent;
        this.executorService = executorService;
    }

    /**
     * Build the default research graph with all nodes and edges
     */
    public ResearchGraph buildDefaultGraph() {
        logger.info("Building default research graph...");

        // Add all nodes
        addNode(NodeType.OPTIMIZE_QUERY, new OptimizeQueryNode(queryOptimizerAgent));
        addNode(NodeType.WEB_SEARCH, new WebSearchNode(webSearchTool));
        addNode(NodeType.QUICK_SUMMARY, new QuickSummaryNode(quickSummaryAgent));
        addNode(NodeType.FETCH_CONTENT, new FetchContentNode(contentFetcherTool));
        addNode(NodeType.COMPREHENSIVE_SUMMARY, new ComprehensiveSummaryNode(summarizingAgent));

        // Define graph edges (flow) - flexible, not strictly DAG
        addEdge(NodeType.OPTIMIZE_QUERY, NodeType.WEB_SEARCH);
        addEdge(NodeType.WEB_SEARCH, NodeType.QUICK_SUMMARY);
        addEdge(NodeType.WEB_SEARCH, NodeType.FETCH_CONTENT);
        addEdge(NodeType.FETCH_CONTENT, NodeType.COMPREHENSIVE_SUMMARY);

        // Note: QUICK_SUMMARY and COMPREHENSIVE_SUMMARY are terminal nodes (return empty from getNextNodes())

        return build();
    }

    /**
     * Add a node to the graph
     */
    public ResearchGraphBuilder addNode(NodeType nodeType, GraphNode node) {
        nodes.put(nodeType, node);
        return this;
    }

    /**
     * Add an edge between two nodes
     */
    public ResearchGraphBuilder addEdge(NodeType from, NodeType to) {
        edges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        return this;
    }

    /**
     * Remove a node and all its edges
     */
    public ResearchGraphBuilder removeNode(NodeType nodeType) {
        nodes.remove(nodeType);
        edges.remove(nodeType);
        edges.values().forEach(list -> list.remove(nodeType));
        return this;
    }

    /**
     * Remove an edge
     */
    public ResearchGraphBuilder removeEdge(NodeType from, NodeType to) {
        List<NodeType> toList = edges.get(from);
        if (toList != null) {
            toList.remove(to);
        }
        return this;
    }

    /**
     * Build the graph
     */
    public ResearchGraph build() {
        ResearchGraph graph = new ResearchGraph(nodes, edges, executorService);

        // Validate the graph
        if (!graph.validate()) {
            throw new IllegalStateException("Invalid graph structure");
        }

        logger.info("Research graph built successfully:");
        logger.info(graph.toString());

        return graph;
    }

    /**
     * Get current nodes (for inspection)
     */
    public Map<NodeType, GraphNode> getNodes() {
        return new HashMap<>(nodes);
    }

    /**
     * Get current edges (for inspection)
     */
    public Map<NodeType, List<NodeType>> getEdges() {
        return new HashMap<>(edges);
    }
}
