package com.ninickname.summarizer.model;

import java.util.ArrayList;
import java.util.List;

public class ResearchResult {
    private String topic;
    private String optimizedQuery;
    private SearxngResponse searchResults;
    private String quickSummary;
    private List<ContentData> structuredContents;
    private String comprehensiveSummary;

    public ResearchResult() {
        this.structuredContents = new ArrayList<>();
    }

    public ResearchResult(String topic) {
        this.topic = topic;
        this.structuredContents = new ArrayList<>();
    }

    // Setters for incremental updates
    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setOptimizedQuery(String optimizedQuery) {
        this.optimizedQuery = optimizedQuery;
    }

    public void setSearchResults(SearxngResponse searchResults) {
        this.searchResults = searchResults;
    }

    public void setQuickSummary(String quickSummary) {
        this.quickSummary = quickSummary;
    }

    public List<ContentData> getStructuredContents() {
        return structuredContents;
    }

    public void setStructuredContents(List<ContentData> structuredContents) {
        this.structuredContents = structuredContents;
    }

    public void setComprehensiveSummary(String comprehensiveSummary) {
        this.comprehensiveSummary = comprehensiveSummary;
    }

    // Getters
    public String getTopic() {
        return topic;
    }

    public String getOptimizedQuery() {
        return optimizedQuery;
    }

    public SearxngResponse getSearchResults() {
        return searchResults;
    }

    public String getQuickSummary() {
        return quickSummary;
    }

    public String getComprehensiveSummary() {
        return comprehensiveSummary;
    }

    @Override
    public String toString() {
        return "ResearchResult{" +
                "topic='" + topic + '\'' +
                ", optimizedQuery='" + optimizedQuery + '\'' +
                ", searchResults=" + (searchResults != null ? searchResults.results().size() + " results" : "null") +
                ", quickSummary=" + (quickSummary != null ? "available" : "null") +
                ", structuredContents=" + (structuredContents != null ? structuredContents.size() + " fetched" : "null") +
                ", comprehensiveSummary=" + (comprehensiveSummary != null ? "available" : "null") +
                '}';
    }
}
