package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface QueryOptimizerAgent {

    @SystemMessage("You are a search query optimization expert. " +
            "Your task is to transform user queries into optimal search queries for web search engines. " +
            "Fix typos, improve clarity, add relevant keywords, and make the query more specific. " +
            "Return ONLY the optimized query text without any explanation or additional formatting. " +
            "Keep it concise and focused on what will give the best search results.")
    @UserMessage("Optimize this search query: {{query}}")
    String optimizeQuery(String query);

    static QueryOptimizerAgent create(ChatModel chatModel) {
        return AiServices.builder(QueryOptimizerAgent.class)
                .chatModel(chatModel)
                .build();
    }
}
