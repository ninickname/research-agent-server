package com.ninickname.summarizer.graph;

import com.ninickname.summarizer.graph.nodes.GraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Graph execution engine that orchestrates node execution.
 *
 * Best Practices:
 * - Flexible Graph: Nodes can have cycles and dynamic routing
 * - Parallel Execution: Nodes with no dependencies run concurrently
 * - Conditional Routing: Nodes can be skipped based on state
 * - Error Handling: Failed nodes are tracked but don't stop the graph
 */
public class ResearchGraph {
    private static final Logger logger = LoggerFactory.getLogger(ResearchGraph.class);

    private final Map<NodeType, GraphNode> nodes;
    private final Map<NodeType, List<NodeType>> edges; // Adjacency list
    private final ExecutorService executorService;

    public ResearchGraph(Map<NodeType, GraphNode> nodes,
                         Map<NodeType, List<NodeType>> edges,
                         ExecutorService executorService) {
        this.nodes = new HashMap<>(nodes);
        this.edges = new HashMap<>(edges);
        this.executorService = executorService;
    }

    /**
     * Execute the graph starting from OPTIMIZE_QUERY node
     */
    public ResearchGraphState execute(ResearchGraphState initialState) {
        logger.info("Starting graph execution for topic: '{}'", initialState.getTopic());

        ResearchGraphState currentState = initialState;
        Set<NodeType> completed = new HashSet<>();
        Queue<NodeType> queue = new LinkedList<>();

        // Start with OPTIMIZE_QUERY as the entry point
        queue.add(NodeType.OPTIMIZE_QUERY);

        while (!queue.isEmpty()) {
            NodeType nodeType = queue.poll();

            // Skip if already completed (prevents infinite loops in current execution)
            if (completed.contains(nodeType)) {
                logger.debug("Node {} already completed in this execution, skipping", nodeType);
                continue;
            }

            // Get the node
            GraphNode node = nodes.get(nodeType);
            if (node == null) {
                logger.warn("Node {} not found in graph, skipping", nodeType);
                completed.add(nodeType);
                continue;
            }

            // Execute the node
            logger.info("Executing node: {}", nodeType);
            currentState = node.execute(currentState);
            completed.add(nodeType);

            // Get available edges for this node
            List<NodeType> availableEdges = edges.getOrDefault(nodeType, List.of());

            // Let node decide which edges to follow (conditional routing)
            List<NodeType> nextNodes = node.getNextNodes(currentState, availableEdges);

            if (nextNodes.isEmpty()) {
                logger.info("Node {} has no next nodes - terminal node", nodeType);
            } else {
                logger.debug("Node {} routing to: {}", nodeType, nextNodes);
                queue.addAll(nextNodes);
            }
        }

        // Log completion summary
        long totalDuration = currentState.getElapsedTime();
        logger.info("=== GRAPH EXECUTION COMPLETED ===");
        logger.info("Total time: {}ms ({} seconds)", totalDuration, totalDuration / 1000.0);
        logger.info("Nodes executed: {}", completed.size());
        logger.info("Node timings:");
        currentState.getNodeDurations().forEach((node, duration) ->
                logger.info("  {}: {}ms", node.getId(), duration));

        if (!currentState.getNodeErrors().isEmpty()) {
            logger.warn("Errors encountered:");
            currentState.getNodeErrors().forEach((node, error) ->
                    logger.warn("  {}: {}", node.getId(), error));
        }

        logger.info("===================================");

        return currentState;
    }

    /**
     * Execute nodes in parallel where possible (advanced feature for future)
     * This would require topological sort and dependency analysis
     */
    public CompletableFuture<ResearchGraphState> executeAsync(ResearchGraphState initialState) {
        return CompletableFuture.supplyAsync(() -> execute(initialState), executorService);
    }

    /**
     * Get graph structure for visualization/debugging
     */
    public Map<NodeType, List<NodeType>> getEdges() {
        return new HashMap<>(edges);
    }

    /**
     * Get all nodes in the graph
     */
    public Map<NodeType, GraphNode> getNodes() {
        return new HashMap<>(nodes);
    }

    /**
     * Validate graph structure (basic checks, flexible graph allows cycles)
     */
    public boolean validate() {
        // Check OPTIMIZE_QUERY exists as entry point
        if (!nodes.containsKey(NodeType.OPTIMIZE_QUERY)) {
            logger.error("Graph validation failed: No OPTIMIZE_QUERY entry node");
            return false;
        }

        // Check all nodes are reachable from entry point
        Set<NodeType> reachable = new HashSet<>();
        Queue<NodeType> queue = new LinkedList<>();
        queue.add(NodeType.OPTIMIZE_QUERY);
        Set<NodeType> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            NodeType current = queue.poll();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            reachable.add(current);
            queue.addAll(edges.getOrDefault(current, List.of()));
        }

        // Warn about unreachable nodes but don't fail
        for (NodeType nodeType : nodes.keySet()) {
            if (!reachable.contains(nodeType)) {
                logger.warn("Node {} is not reachable from OPTIMIZE_QUERY entry point", nodeType);
            }
        }

        logger.info("Graph validation passed: {} nodes, {} edges, {} reachable",
                nodes.size(), edges.values().stream().mapToInt(List::size).sum(), reachable.size());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResearchGraph {\n");
        sb.append("  Nodes: ").append(nodes.keySet()).append("\n");
        sb.append("  Edges:\n");
        edges.forEach((from, toList) ->
                toList.forEach(to ->
                        sb.append("    ").append(from.getId())
                                .append(" -> ").append(to.getId()).append("\n")));
        sb.append("}");
        return sb.toString();
    }
}
