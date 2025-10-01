//package com.ninickname.summarizer;
//
//import com.ninickname.summarizer.model.SearchResult;
//import com.ninickname.summarizer.tool.WebSearchTool;
//import com.ninickname.summarizer.tool.ContentFetcherTool;
//import com.ninickname.summarizer.agents.SummarizingAgent;
//import dev.langchain4j.model.chat.ChatLanguageModel;
//import dev.langchain4j.model.chat.ChatModel;
//import dev.langchain4j.model.ollama.OllamaChatModel;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//public class SimpleResearchAgentTest {
//
//    @Test
//    void testDirectToolUsage() {
//        System.out.println("Testing direct tool usage (bypassing agent layer):");
//
//        // Use tools directly
//        WebSearchTool webSearchTool = WebSearchTool.create();
//        ContentFetcherTool contentTool = new ContentFetcherTool();
//
//        // Create LLM for summarization
//        ChatModel chatModel = OllamaChatModel.builder()
//                .baseUrl("http://localhost:11434")
//                .modelName("llama3.1:latest")
//                .build();
//        SummarizingAgent summaryAgent = SummarizingAgent.create(chatModel);
//
//        String topic = "artificial intelligence";
//        int maxResults = 3;
//
//        try {
//            // Step 1: Direct search
//            System.out.println("Step 1: Searching directly...");
//            List<SearchResult> searchResults = webSearchTool.search(topic, maxResults);
//            System.out.println("Found " + searchResults.size() + " search results");
//
//            if (!searchResults.isEmpty()) {
//                // Step 2: Direct content fetching
//                System.out.println("Step 2: Fetching content directly...");
//                List<String> urls = searchResults.stream()
//                        .map(SearchResult::getUrl)
//                        .toList();
//                List<String> contents = contentTool.fetchMultipleContents(urls);
//                System.out.println("Fetched " + contents.size() + " content items");
//
//                // Step 3: Direct summarization
//                System.out.println("Step 3: Summarizing directly...");
//                String prompt = SummarizingAgent.buildPrompt(topic, contents);
//                String summary = summaryAgent.summarizeResearch(prompt);
//                System.out.println("Summary: " + summary);
//            } else {
//                System.out.println("No search results found - likely MCP server not running");
//            }
//
//            System.out.println("Direct tool usage test completed successfully!");
//
//        } catch (Exception e) {
//            System.err.println("Error in direct tool usage: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}