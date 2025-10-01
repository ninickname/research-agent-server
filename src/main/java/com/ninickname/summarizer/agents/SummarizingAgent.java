package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

public interface SummarizingAgent {

    @SystemMessage("You are a research summarization agent." +
            "Your task is to analyze and summarize information from multiple web sources about a given topic. " +
            "Create a comprehensive, accurate summary that synthesizes the key points from all provided sources. " +
            "Base your summary ONLY on the information provided in the sources - do not add external knowledge. " +
            "Structure your summary clearly with the most important information first.")
    String summarizeResearch(@UserMessage String prompt);

    static SummarizingAgent create(ChatModel chatModel) {
        return AiServices.builder(SummarizingAgent.class)
                .chatModel(chatModel)
                .build();
    }

    static String buildPrompt(String topic, List<String> contents) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Topic: ").append(topic).append("\n\n");
        prompt.append("Please summarize the following ").append(contents.size()).append(" sources about this topic:\n\n");

        for (int i = 0; i < contents.size(); i++) {
            prompt.append("--- Source ").append(i + 1).append(" ---\n");
            prompt.append(contents.get(i));
            prompt.append("\n\n");
        }

        prompt.append("Based on these sources, provide a comprehensive summary of the topic.");
        return prompt.toString();
    }
}