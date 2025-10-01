package com.ninickname.summarizer.tool;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninickname.summarizer.model.SearxngResponse;
import dev.langchain4j.agent.tool.Tool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class WebSearchTool {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchTool.class);
    private final ObjectMapper objectMapper;

    public WebSearchTool(@Value("${mcp.web.url:http://localhost:9101}") String mcpUrl) {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        logger.info("WebSearchTool initialized with MCP URL: {}", mcpUrl);
    }

    @Tool("Search the web for information about a given topic with specified number of results")
    public SearxngResponse search(String query, int maxResults) {
        logger.info("Searching for: {} (max results: {})", query, maxResults);

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder("http://localhost:9101").build();
        try (McpSyncClient client = McpClient
                .sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build()) {

            client.initialize();

            // Use the client
//                var tools = client.listTools();
            McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder().name("web_search")
                    .arguments(Map.of("query", query, "p", maxResults)).build();
            McpSchema.CallToolResult responseFromTool = client.callTool(toolRequest);
            String textualResponse = ((McpSchema.TextContent) responseFromTool.content().get(0)).text();

            System.out.println("Tool response: " + textualResponse);
            logger.debug("MCP response: {}", textualResponse);
            SearxngResponse ret = objectMapper.readValue(textualResponse, SearxngResponse.class);
            logger.info("Found {} search results", ret.results().size());
            return ret;
        } catch (Exception e) {
            logger.error("Search failed", e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }
}
