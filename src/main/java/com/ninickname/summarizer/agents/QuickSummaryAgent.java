package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface QuickSummaryAgent {

    @SystemMessage("You are a quick summarization agent. " +
            "Your task is to create a preliminary summary based on search result snippets. " +
            "This is a QUICK, partial answer - not comprehensive. " +
            "Synthesize the key points from the snippets provided. " +
            "Be concise and acknowledge this is preliminary information.")
    String summarizeSnippets(@UserMessage String prompt);

    static QuickSummaryAgent create(ChatModel chatModel) {
        return AiServices.builder(QuickSummaryAgent.class)
                .chatModel(chatModel)
                .build();
    }

    static String buildPrompt(String topic, List<String> snippets) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Topic: ").append(topic).append("\n\n");
        prompt.append("Create a quick preliminary summary from these ").append(snippets.size()).append(" search snippets:\n\n");

        for (int i = 0; i < snippets.size(); i++) {
            prompt.append("- Snippet ").append(i + 1).append(": ");
            prompt.append(snippets.get(i));
            prompt.append("\n");
        }

        prompt.append("\nProvide a brief preliminary summary. Note that this is based on snippets only.");
        return prompt.toString();
    }
}
