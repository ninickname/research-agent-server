package com.ninickname.summarizer.graph.nodes;

import com.ninickname.summarizer.agents.QueryOptimizerAgent;
import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;

/**
 * Node that optimizes the user's search query using AI.
 */
public class OptimizeQueryNode extends BaseGraphNode {
    private final QueryOptimizerAgent queryOptimizerAgent;

    public OptimizeQueryNode(QueryOptimizerAgent queryOptimizerAgent) {
        super(NodeType.OPTIMIZE_QUERY);
        this.queryOptimizerAgent = queryOptimizerAgent;
    }

    @Override
    protected ResearchGraphState executeInternal(ResearchGraphState state) {
        String topic = state.getTopic();
        logger.info("Optimizing query for topic: '{}'", topic);

        String optimizedQuery = queryOptimizerAgent.optimizeQuery(topic);

        logger.info("Query optimized: '{}' -> '{}'", topic, optimizedQuery);
        emitProgress(state, "optimized_query", optimizedQuery);

        return state.toBuilder()
                .optimizedQuery(optimizedQuery)
                .build();
    }
}
