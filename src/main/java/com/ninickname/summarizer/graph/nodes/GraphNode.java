package com.ninickname.summarizer.graph.nodes;

import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;

import java.util.List;

/**
 * Base interface for all graph nodes.
 *
 * Best Practices:
 * - Functional: Each node is a pure function (state in -> state out)
 * - Immutable: Returns new state, never modifies input
 * - Single Responsibility: Each node does one thing
 * - Composable: Nodes can be chained, parallelized, or conditionally executed
 */
public interface GraphNode {
    /**
     * Execute this node's logic and return updated state
     *
     * @param state Current graph state
     * @return New state with this node's updates
     */
    ResearchGraphState execute(ResearchGraphState state);

    /**
     * Get the type of this node (for logging, routing, etc.)
     */
    NodeType getNodeType();

    /**
     * Get human-readable description of this node
     */
    default String getDescription() {
        return getNodeType().getId();
    }

    /**
     * Determine which edges to follow based on state (conditional routing)
     *
     * @param state Current graph state
     * @param availableEdges All edges defined in graph for this node
     * @return List of next nodes to execute (empty = terminal node)
     */
    default List<NodeType> getNextNodes(ResearchGraphState state, List<NodeType> availableEdges) {
        return availableEdges; // Follow all edges by default
    }
}
