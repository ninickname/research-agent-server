package com.ninickname.summarizer.model;

import com.ninickname.summarizer.tool.WebSearchTool;

import java.util.List;

public class ResearchResult {
    private final String topic;
    private final SearxngResponse searchResults;
    private final List<String> contents;
    private final String summary;

    public ResearchResult(String topic, SearxngResponse searchResults,
                         List<String> contents, String summary) {
        this.topic = topic;
        this.searchResults = searchResults;
        this.contents = List.copyOf(contents);
        this.summary = summary;
    }

    public String getTopic() {
        return topic;
    }

    public SearxngResponse getSearchResults() {
        return searchResults;
    }

    public List<String> getContents() {
        return contents;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "ResearchResult{" +
                "topic='" + topic + '\'' +
                ", searchResults=" + searchResults.results().size() + " results" +
                ", contents=" + contents.size() + " fetched" +
                ", summary='" + summary.substring(0, Math.min(100, summary.length())) + "...'" +
                '}';
    }
}