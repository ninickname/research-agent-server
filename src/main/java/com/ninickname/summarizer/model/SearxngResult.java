package com.ninickname.summarizer.model;

public record SearxngResult(
        String url,
        String title,
        String content,
        String engine,
        Double score
) {
}
