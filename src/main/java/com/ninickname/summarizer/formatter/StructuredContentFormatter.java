package com.ninickname.summarizer.formatter;

import com.ninickname.summarizer.model.ContentData;

public class StructuredContentFormatter {

    /**
     * Convert ContentData (record) to formatted string for LLM consumption
     */
    public static String toFormattedString(ContentData content) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== ").append(content.title() != null ? content.title() : "Content").append(" ===\n");
        sb.append("Source: ").append(content.url()).append("\n\n");

        if (content.mainHeading() != null && !content.mainHeading().isEmpty()) {
            sb.append("## ").append(content.mainHeading()).append("\n\n");
        }

        for (ContentData.SectionData section : content.sections()) {
            if (section.heading() != null && !section.heading().isEmpty()) {
                sb.append("### ").append(section.heading()).append("\n");
            }

            if (section.content() != null && !section.content().isEmpty()) {
                sb.append(section.content()).append("\n");
            }

            // Add subsections
            if (!section.subSections().isEmpty()) {
                for (ContentData.SubSectionData subSection : section.subSections()) {
                    sb.append("\n");
                    if (subSection.heading() != null && !subSection.heading().isEmpty()) {
                        sb.append("#### ").append(subSection.heading()).append("\n");
                    }

                    if (subSection.content() != null && !subSection.content().isEmpty()) {
                        sb.append(subSection.content()).append("\n");
                    }
                }
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
