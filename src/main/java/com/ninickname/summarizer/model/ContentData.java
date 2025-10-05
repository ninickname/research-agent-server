package com.ninickname.summarizer.model;

import java.util.List;

/**
 * Immutable record-based representation of structured content
 */
public record ContentData(
        String url,
        String title,
        String mainHeading,
        List<SectionData> sections,
        int totalCharacters,
        boolean hasStructure,
        String engine,
        Double score
) {
    public record SectionData(
            String heading,
            String content,
            List<SubSectionData> subSections
    ) {}

    public record SubSectionData(
            String heading,
            String content
    ) {}
}
