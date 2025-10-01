package com.ninickname.summarizer.model;

import java.util.List;

public record SearxngResponse(
        String query,
        List<SearxngResult> results,
        List<String> suggestions
) {
}
