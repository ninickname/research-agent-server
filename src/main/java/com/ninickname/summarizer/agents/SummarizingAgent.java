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
            "Structure your summary clearly with the most important information first.")
    @UserMessage("Summarize the following sources about the topic '{{topic}}':\n\nSources: {{sources}}")
    String summarizeResearch(@V("topic") String topic, @V("sources") List<String> sources);

    static SummarizingAgent create(ChatModel chatModel) {
        return AiServices.builder(SummarizingAgent.class)
                .chatModel(chatModel)
                .build();
    }
}