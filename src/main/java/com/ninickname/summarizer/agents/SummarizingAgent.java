package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface SummarizingAgent {

    @SystemMessage("You are a research summarization agent. " +
            "Your task is to analyze and summarize information from multiple web sources about a given topic. " +
            "Create a comprehensive, accurate summary that synthesizes the key points from all provided sources. " +
            "Base your summary ONLY on the information provided in the sources - do not add external knowledge. " +
            "\n\n" +
            "IMPORTANT - Markdown Formatting Rules:\n" +
            "1. Use proper heading syntax: ## for main sections, ### for subsections, #### for sub-subsections\n" +
            "2. NEVER use bold text (**text**) as headings - always use # symbols\n" +
            "3. Always add a blank line before and after headings\n" +
            "4. Always add a blank line before lists\n" +
            "5. Use proper list syntax: - for bullet points, 1. for numbered lists\n" +
            "6. Add blank lines between different sections\n" +
            "7. ALWAYS end with a '## Sources' section listing all source URLs\n" +
            "\n" +
            "Example of CORRECT formatting:\n" +
            "## Key Features\n" +
            "\n" +
            "- Feature one with detailed explanation\n" +
            "- Feature two with context\n" +
            "\n" +
            "### Technical Details\n" +
            "\n" +
            "More information here.\n" +
            "\n" +
            "## Sources\n" +
            "\n" +
            "1. https://example.com/source1\n" +
            "2. https://example.com/source2\n" +
            "\n" +
            "Example of INCORRECT formatting (DO NOT DO THIS):\n" +
            "**Key Features** (wrong - use ## instead)\n" +
            "- Feature one (wrong - needs blank line before list)")
    @UserMessage("Summarize the following sources about the topic '{{topic}}':\n\nSources: {{sources}}\n\n" +
            "Source URLs: {{sourceUrls}}\n\n" +
            "Remember: Use proper Markdown headings (##, ###) and blank lines for clean formatting! " +
            "IMPORTANT: End your summary with a '## Sources' section listing all the source URLs as a numbered list to give proper credit.")
    String summarizeResearch(@V("topic") String topic, @V("sources") List<String> sources, @V("sourceUrls") List<String> sourceUrls);

    static SummarizingAgent create(ChatModel chatModel) {
        return AiServices.builder(SummarizingAgent.class)
                .chatModel(chatModel)
                .build();
    }
}