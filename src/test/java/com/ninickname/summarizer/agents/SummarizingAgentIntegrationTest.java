package com.ninickname.summarizer.agents;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class SummarizingAgentIntegrationTest {

    private SummarizingAgent agent;
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        String ollamaUrl = System.getProperty("ollama.url", "http://localhost:11434");
        chatModel = OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName("llama3.1:latest")
                .build();

        agent = SummarizingAgent.create(chatModel);
    }

    @Test
    void testSummarizeResearch() {
        String topic = "artificial intelligence";
        List<String> contents = Arrays.asList(
                "Artificial Intelligence (AI) is a broad field of computer science focused on creating systems capable of performing tasks that typically require human intelligence. These tasks include learning, reasoning, problem-solving, perception, and language understanding.",
                "Machine learning is a subset of AI that involves training algorithms on data to enable them to make predictions or decisions without being explicitly programmed for every scenario. Deep learning, which uses neural networks with multiple layers, has been particularly successful in recent years.",
                "AI applications are widespread today, including recommendation systems, autonomous vehicles, natural language processing, computer vision, and robotics. Major tech companies like Google, Microsoft, and OpenAI are investing heavily in AI research and development."
        );

        String prompt = SummarizingAgent.buildPrompt(topic, contents);

        System.out.println("Testing SummarizingAgent with topic: " + topic);
        System.out.println("Prompt length: " + prompt.length() + " characters");
        System.out.println("Content sources: " + contents.size());

        try {
            String summary = agent.summarizeResearch(prompt);

            System.out.println("Generated summary:");
            System.out.println(summary);

            assertNotNull(summary);
            assertFalse(summary.trim().isEmpty());
            assertTrue(summary.length() > 50); // Should be a meaningful summary

            // Verify it mentions the topic
            assertTrue(summary.toLowerCase().contains("artificial intelligence") ||
                      summary.toLowerCase().contains("ai"));

        } catch (Exception e) {
            System.err.println("Error during summarization: " + e.getMessage());
            e.printStackTrace();
            fail("Summarization failed with exception: " + e.getMessage());
        }
    }

    @Test
    void testBuildPrompt() {
        String topic = "machine learning";
        List<String> contents = Arrays.asList(
                "Content 1 about machine learning",
                "Content 2 about neural networks"
        );

        String prompt = SummarizingAgent.buildPrompt(topic, contents);

        System.out.println("Built prompt:");
        System.out.println(prompt);

        assertNotNull(prompt);
        assertTrue(prompt.contains(topic));
        assertTrue(prompt.contains("Content 1 about machine learning"));
        assertTrue(prompt.contains("Content 2 about neural networks"));
        assertTrue(prompt.contains("Source 1"));
        assertTrue(prompt.contains("Source 2"));
    }
}