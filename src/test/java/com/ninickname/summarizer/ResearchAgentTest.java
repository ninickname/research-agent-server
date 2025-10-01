//package com.ninickname.summarizer;
//
//import com.ninickname.summarizer.agents.SummarizingAgent;
//import com.ninickname.summarizer.tool.WebSearchTool;
//import com.ninickname.summarizer.tool.ContentFetcherTool;
//import com.ninickname.summarizer.model.ResearchResult;
//import com.ninickname.summarizer.model.SearchResult;
//import com.ninickname.summarizer.service.ResearchOrchestrator;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class ResearchAgentTest {
//
//    private ResearchOrchestrator orchestrator;
//    private MockWebSearchTool mockWebSearchTool;
//    private MockContentFetcherTool mockContentFetcherTool;
//    private MockSummarizingAgent mockSummarizingAgent;
//
//    @BeforeEach
//    void setUp() {
//        mockWebSearchTool = new MockWebSearchTool();
//        mockContentFetcherTool = new MockContentFetcherTool();
//        mockSummarizingAgent = new MockSummarizingAgent();
//
//        orchestrator = new ResearchOrchestrator(mockWebSearchTool, mockContentFetcherTool, mockSummarizingAgent);
//    }
//
//    @Test
//    void testResearchWithValidTopic() {
//        String topic = "artificial intelligence";
//        ResearchResult result = orchestrator.research(topic, 3);
//
//        assertNotNull(result);
//        assertEquals(topic, result.getTopic());
//        assertEquals(3, result.getSearchResults().size());
//        assertEquals(3, result.getContents().size());
//        assertTrue(result.getSummary().contains("Mock summary"));
//    }
//
//    @Test
//    void testResearchWithDefaultCount() {
//        String topic = "machine learning";
//        ResearchResult result = orchestrator.research(topic, 5);
//
//        assertNotNull(result);
//        assertEquals(topic, result.getTopic());
//        assertEquals(5, result.getSearchResults().size());
//    }
//
//    // Mock implementations for testing
//    private static class MockWebSearchTool implements WebSearchTool {
//        @Override
//        public List<SearchResult> search(String query, int maxResults) {
//            return IntStream.range(0, maxResults)
//                    .mapToObj(i -> new SearchResult(
//                            "Title " + i,
//                            "https://example.com/" + i,
//                            "Description " + i
//                    ))
//                    .collect(Collectors.toList());
//        }
//    }
//
//    private static class MockContentFetcherTool extends ContentFetcherTool {
//        @Override
//        public List<String> fetchMultipleContents(List<String> urls) {
//            return urls.stream()
//                    .map(url -> "Mock content from " + url)
//                    .collect(Collectors.toList());
//        }
//    }
//
//    private static class MockSummarizingAgent implements SummarizingAgent {
//        @Override
//        public String summarizeResearch(String prompt) {
//            return "Mock summary based on provided research content";
//        }
//    }
//}