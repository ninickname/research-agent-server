package com.ninickname.summarizer.graph;

/**
 * Research graph node types.
 * Defines all steps in the research workflow.
 */
public enum NodeType {
    OPTIMIZE_QUERY("optimizing_query"),
    WEB_SEARCH("searching"),
    QUICK_SUMMARY("quick_summary"),
    FETCH_CONTENT("fetching_content"),
    COMPREHENSIVE_SUMMARY("comprehensive_summary"),
    ROUTER("router");

    private final String id;

    NodeType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
