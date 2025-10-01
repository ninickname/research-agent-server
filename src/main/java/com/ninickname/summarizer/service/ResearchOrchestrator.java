package com.ninickname.summarizer.service;

import com.ninickname.summarizer.agents.SummarizingAgent;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.model.SearxngResponse;
import com.ninickname.summarizer.model.SearxngResult;
import com.ninickname.summarizer.tool.ContentFetcherTool;
import com.ninickname.summarizer.tool.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResearchOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(ResearchOrchestrator.class);

    private final WebSearchTool webSearchTool;
    private final ContentFetcherTool contentFetcherTool;
    private final SummarizingAgent summarizingAgent;

    public ResearchOrchestrator(WebSearchTool webSearchTool,
                                ContentFetcherTool contentFetcherTool,
                                SummarizingAgent summarizingAgent) {
        this.webSearchTool = webSearchTool;
        this.contentFetcherTool = contentFetcherTool;
        this.summarizingAgent = summarizingAgent;
    }

    public ResearchResult research(String topic, int resultCount) {
        logger.info("Starting research for topic: {} with {} results", topic, resultCount);

        try {
            // Step 1: Direct search for relevant URLs
            logger.info("Searching for topic using WebSearchTool...");
            SearxngResponse searchResults = webSearchTool.search(topic, resultCount);
            logger.info("WebSearchTool found {} search results", searchResults.results().size());

            // Step 2: Direct content fetching from URLs
            List<String> urls = searchResults.results().stream()
                    .map(SearxngResult::url)
                    .toList();

            logger.info("Fetching content using ContentFetcherTool...");
            List<String> contents = contentFetcherTool.fetchMultipleContents(urls);
            logger.info("ContentFetcherTool successfully fetched content from {} sources", contents.size());

            // final summary
            String prompt = SummarizingAgent.buildPrompt(topic, contents.subList(0,5));
            logger.info("Sending summarization request to SummarizingAgent...");
            String summary = summarizingAgent.summarizeResearch(prompt);
            logger.info("SummarizingAgent completed summarization");

            return new ResearchResult(topic, searchResults, contents, summary);

        } catch (Exception e) {
            logger.error("Error during research for topic: {}", topic, e);
            throw new RuntimeException("Research failed: " + e.getMessage(), e);
        }
    }
}