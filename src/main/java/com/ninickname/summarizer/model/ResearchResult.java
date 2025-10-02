package com.ninickname.summarizer.model;

import java.util.ArrayList;
import java.util.List;

public class ResearchResult {
    private String topic;
    private String optimizedQuery;
    private SearxngResponse searchResults;
    private String quickSummary;
    private List<String> contents;
    private String comprehensiveSummary;

    public ResearchResult() {
        this.contents = new ArrayList<>();
    }

    public ResearchResult(String topic) {
        this.topic = topic;
        this.contents = new ArrayList<>();
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

    public void setContents(List<String> contents) {
        this.contents = new ArrayList<>(contents);
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

    public List<String> getContents() {
        return contents;
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
                ", contents=" + contents.size() + " fetched" +
                ", comprehensiveSummary=" + (comprehensiveSummary != null ? "available" : "null") +
                '}';
    }
}