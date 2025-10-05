package com.ninickname.summarizer.graph.nodes;

import com.ninickname.summarizer.agents.QuickSummaryAgent;
import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;
import com.ninickname.summarizer.model.SearxngResult;

import java.util.List;

/**
 * Node that generates a quick summary from search snippets.
 * Terminal node for now (no outgoing edges).
 */
public class QuickSummaryNode extends BaseGraphNode {
    private final QuickSummaryAgent quickSummaryAgent;

    public QuickSummaryNode(QuickSummaryAgent quickSummaryAgent) {
        super(NodeType.QUICK_SUMMARY);
        this.quickSummaryAgent = quickSummaryAgent;
    }

    @Override
    public List<NodeType> getNextNodes(ResearchGraphState state, List<NodeType> availableEdges) {
        // QuickSummary is terminal for now (no next nodes)
        return List.of();
    }

    @Override
    protected ResearchGraphState executeInternal(ResearchGraphState state) {
        // Extract snippets from search results
        List<String> snippets = state.getSearchResults().results().stream()
                .map(SearxngResult::content)
                .filter(content -> content != null && !content.trim().isEmpty())
                .toList();

        logger.info("Generating quick summary from {} snippets", snippets.size());

        String prompt = QuickSummaryAgent.buildPrompt(state.getTopic(), snippets);
        String quickSummary = quickSummaryAgent.summarizeSnippets(prompt);

        logger.info("Quick summary generated");
        emitProgress(state, "quick_summary", quickSummary);

        return state.toBuilder()
                .quickSummary(quickSummary)
                .build();
    }
}
