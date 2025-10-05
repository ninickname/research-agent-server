package com.ninickname.summarizer.graph.nodes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;
import com.ninickname.summarizer.model.ContentData;
import com.ninickname.summarizer.model.SearxngResult;
import com.ninickname.summarizer.tool.ContentFetcherTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Node that fetches structured content from URLs.
 */
public class FetchContentNode extends BaseGraphNode {
    private final ContentFetcherTool contentFetcherTool;
    private final ObjectMapper objectMapper;

    public FetchContentNode(ContentFetcherTool contentFetcherTool) {
        super(NodeType.FETCH_CONTENT);
        this.contentFetcherTool = contentFetcherTool;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<NodeType> getNextNodes(ResearchGraphState state, List<NodeType> availableEdges) {
        // If skipContentFetch is set, don't follow any edges (terminal node)
        if (state.isSkipContentFetch()) {
            logger.info("skipContentFetch=true, FetchContentNode is terminal");
            return List.of(); // No next nodes - terminal
        }
        return availableEdges; // Otherwise follow all edges
    }

    @Override
    protected ResearchGraphState executeInternal(ResearchGraphState state) {
        int resultCount = state.getResultCount();
        var searchResults = state.getSearchResults();

        // Create URL to metadata map
        var urlToSearchResult = searchResults.results().stream()
                .collect(Collectors.toMap(
                        SearxngResult::url,
                        r -> r,
                        (a, b) -> a // Keep first in case of duplicates
                ));

        List<String> allUrls = searchResults.results().stream()
                .map(SearxngResult::url)
                .toList();

        logger.info("Fetching structured content (target: {} sources, available: {} URLs)",
                resultCount, allUrls.size());

        // Fetch in batches until we have enough content
        List<ContentData> structuredContents = new ArrayList<>();
        int batchSize = Math.min(resultCount * 2, allUrls.size());
        int urlIndex = 0;

        while (structuredContents.size() < resultCount && urlIndex < allUrls.size()) {
            int endIndex = Math.min(urlIndex + batchSize, allUrls.size());
            List<String> urlBatch = allUrls.subList(urlIndex, endIndex);

            logger.info("Fetching batch of {} URLs (current: {}/{}, index: {}-{})",
                    urlBatch.size(), structuredContents.size(), resultCount, urlIndex, endIndex);

            var batchResults = contentFetcherTool.fetchMultipleStructuredContents(urlBatch);

            // Filter and enrich content
            for (ContentData content : batchResults) {
                if (content == null || content.totalCharacters() < 150) {
                    logger.debug("Skipping low-quality content ({} chars)",
                            content != null ? content.totalCharacters() : 0);
                    continue;
                }

                // Enrich with search metadata
                SearxngResult searchResult = urlToSearchResult.get(content.url());
                if (searchResult != null) {
                    ContentData enrichedContent = new ContentData(
                            content.url(),
                            content.title(),
                            content.mainHeading(),
                            content.sections(),
                            content.totalCharacters(),
                            content.hasStructure(),
                            searchResult.engine(),
                            searchResult.score()
                    );
                    structuredContents.add(enrichedContent);
                } else {
                    structuredContents.add(content);
                }
            }

            urlIndex = endIndex;

            if (structuredContents.size() >= resultCount) {
                break;
            }

            // Adjust batch size for remaining URLs
            batchSize = resultCount - structuredContents.size() + 2;
        }

        // Limit to requested count
        if (structuredContents.size() > resultCount) {
            structuredContents = structuredContents.subList(0, resultCount);
        }

        logger.info("Fetched {} structured contents (target: {})",
                structuredContents.size(), resultCount);

        // Emit for SSE
        try {
            emitProgress(state, "structured_contents", objectMapper.writeValueAsString(structuredContents));
        } catch (Exception e) {
            logger.warn("Failed to serialize structured contents: {}", e.getMessage());
        }

        return state.toBuilder()
                .structuredContents(structuredContents)
                .build();
    }
}
