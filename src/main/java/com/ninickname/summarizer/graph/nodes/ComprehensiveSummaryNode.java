package com.ninickname.summarizer.graph.nodes;

import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.formatter.StructuredContentFormatter;
import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;
import com.ninickname.summarizer.model.ContentData;

import java.util.List;

/**
 * Node that creates a comprehensive summary from structured content.
 */
public class ComprehensiveSummaryNode extends BaseGraphNode {
    private final SummarizingAgent summarizingAgent;

    public ComprehensiveSummaryNode(SummarizingAgent summarizingAgent) {
        super(NodeType.COMPREHENSIVE_SUMMARY);
        this.summarizingAgent = summarizingAgent;
    }

    @Override
    public List<NodeType> getNextNodes(ResearchGraphState state, List<NodeType> availableEdges) {
        // This node is always terminal (no next nodes)
        return List.of();
    }

    @Override
    protected ResearchGraphState executeInternal(ResearchGraphState state) {
        List<ContentData> structuredContents = state.getStructuredContents();

        // Handle edge case: no content available
        if (structuredContents == null || structuredContents.isEmpty()) {
            logger.warn("No structured contents available for comprehensive summary");
            String fallbackSummary = "Unable to generate comprehensive summary - no content fetched.";
            if (state.getQuickSummary() != null) {
                fallbackSummary += "\n\nQuick summary: " + state.getQuickSummary();
            }
            emitProgress(state, "comprehensive_summary", fallbackSummary);
            return state.toBuilder()
                    .comprehensiveSummary(fallbackSummary)
                    .build();
        }

        logger.info("Creating comprehensive summary from {} sources", structuredContents.size());

        // Convert to formatted strings
        List<String> formattedContents = structuredContents.stream()
                .map(StructuredContentFormatter::toFormattedString)
                .toList();

        // Extract source URLs
        List<String> sourceUrls = structuredContents.stream()
                .map(ContentData::url)
                .toList();

        String comprehensiveSummary = summarizingAgent.summarizeResearch(
                state.getTopic(),
                formattedContents,
                sourceUrls
        );

        logger.info("Comprehensive summary generated");
        emitProgress(state, "comprehensive_summary", comprehensiveSummary);

        return state.toBuilder()
                .comprehensiveSummary(comprehensiveSummary)
                .build();
    }
}
