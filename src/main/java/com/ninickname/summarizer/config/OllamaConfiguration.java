package com.ninickname.summarizer.config;

import com.ninickname.summarizer.agents.SummarizingAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfiguration {

    @Bean
    public ChatModel chatLanguageModel(
            @Value("${ollama.url:http://localhost:11434}") String ollamaUrl,
            @Value("${ollama.model:llama3.1:latest}") String ollamaModel) {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaModel)
                .build();
    }

    @Bean
    public SummarizingAgent summarizingAgent(ChatModel chatLanguageModel) {
        return SummarizingAgent.create(chatLanguageModel);
    }
}