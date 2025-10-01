package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResearchAgent {

    String research(@UserMessage("Research the topic: {topic} using {resultCount} search results") @V("topic") String topic, @V("resultCount") int resultCount);

    static ResearchAgent create(ChatModel chatModel, Object... tools) {
        return AiServices.builder(ResearchAgent.class)
                .chatModel(chatModel)
                .tools(tools)
                .build();
    }
}