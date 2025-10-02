# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Research Agent Server** (`research-agent-server`) - A Spring Boot REST API service using LangChain4j for intelligent web research and content synthesis. The service provides a clean, agent-based architecture to research topics, fetch web content, and generate AI-powered summaries.

**Package:** `com.ninickname.summarizer`

## Development Principles

**IMPORTANT: Consistency Rule**
- **Whenever you correct an error, assume it may appear in other places too**
- **Scan your output before giving it back and make sure the fix is applied consistently**
- This applies to LangChain4j annotations, imports, method signatures, configuration patterns, etc.

## Development Setup

This is a Spring Boot 3.2 + Maven project using LangChain4j for AI integration.

**Prerequisites:**
- Java 17+
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)
- Ollama running with llama3.1:latest model (runs separately, not in Docker)
- MCP web server (included in docker-compose.yml)

## Architecture

The service follows a **Spring Boot microservice architecture** with LangChain4j integration:

### Core Components:

**REST Layer:**
- **ResearchController**: REST API endpoints at `/api/research` (standard JSON) and `/api/research/stream` (SSE streaming)

**Service Layer:**
- **ResearchOrchestrator**: Spring `@Service` that coordinates the research workflow with async processing and SSE support

**Agents (LangChain4j AiService interfaces):**
- **QueryOptimizerAgent**: AI-powered search query refinement for better results
- **QuickSummaryAgent**: Fast preliminary summarization from search snippets
- **SummarizingAgent**: Comprehensive summarization from full content

**Tools (LangChain4j @Tool implementations):**
- **WebSearchTool**: Direct MCP web server communication with integrated JSON parsing
- **ContentFetcherTool**: Self-contained parallel web content fetching with Java HttpClient and JSoup, includes content cleaning and main content extraction

**Configuration:**
- **OllamaConfiguration**: Spring `@Configuration` for beans (ChatLanguageModel, tools, agents)

**Model Classes:**
- **SearxngResult**, **SearxngResponse**, and **ResearchResult**: Data transfer objects

**Web UI:**
- **index.html**: Interactive web interface with real-time SSE progress tracking, collapsible sections, and markdown rendering

### Workflow:
1. REST API request → **ResearchController** (standard JSON or SSE streaming)
2. **ResearchOrchestrator** → **QueryOptimizerAgent** (AI-powered query optimization)
3. **ResearchOrchestrator** → **WebSearchTool** (search with optimized query)
4. **ResearchOrchestrator** → **QuickSummaryAgent** (async preliminary summary from snippets)
5. **ResearchOrchestrator** → **ContentFetcherTool** (parallel full content fetching with cleaning)
6. **ResearchOrchestrator** → **SummarizingAgent** (comprehensive summary from full content)
7. JSON response with complete research results (or SSE events for streaming)

### Configuration Properties:
- **server.port**: 8080
- **ollama.url**: http://localhost:11434 (or http://host.docker.internal:11434 in Docker)
- **ollama.model**: llama3.1:latest
- **mcp.web.url**: http://localhost:9101 (or http://mcp-web:9101 in Docker)

## Common Commands

### Local Development (without Docker)

```bash
# Ensure Ollama is running with llama3.1:latest (must be pre-installed)
ollama run llama3.1:latest

# Ensure MCP web server is running on port 9101

# Build project
mvn clean compile

# Run tests
mvn test

# Run Spring Boot application
mvn spring-boot:run

# Test the API (standard endpoint)
curl "http://localhost:8080/api/research?topic=artificial%20intelligence&count=5"

# Test the streaming endpoint (SSE)
curl -N "http://localhost:8080/api/research/stream?topic=artificial%20intelligence&count=5"

# Access the web UI
open http://localhost:8080

# Package JAR
mvn clean package
```

### Docker Deployment

```bash
# Ensure Ollama is running on host machine
ollama run llama3.1:latest

# Build and start services (MCP web + Research Agent Server)
docker-compose up --build

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f research-agent-server

# Stop services
docker-compose down

# Test the API
curl "http://localhost:8080/api/research?topic=why%20sugar%20is%20good&count=3"

# Access the web UI
open http://localhost:8080
```

### Docker Build Only

```bash
# Build Docker image
docker build -t research-agent-server:latest .

# Run container with external Ollama
docker run -p 8080:8080 \
  -e OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -e MCP_WEB_URL=http://mcp-web:9101 \
  research-agent-server:latest
```



## Implementation Guidelines

### Architecture Requirements:
- **Spring Boot Architecture**: REST controller → Service layer → Tools/Agents
- **Streamlined Design**: Direct tool usage in orchestrator, agents only for LLM reasoning
- **Dependency Injection**: All components managed by Spring IoC container
- **Tool Integration**: Use @Tool annotated methods for reusable functionality
- **Clean Abstractions**: Eliminate unnecessary abstraction layers
- **Separation of Concerns**: Each component has a single, focused responsibility

### LangChain4j Specific:
- **Agent Parameters**: All agent interface method parameters MUST be annotated with @UserMessage, @V, @UserName, or @MemoryId
- **Tool Implementation**: Tools can be used directly or bound to agents via AiServices.builder().tools()
- **Chat Models**: Required for agents that perform LLM reasoning (like SummarizingAgent)
- **Bean Configuration**: ChatLanguageModel, tools, and agents configured as Spring beans

### Spring Boot Patterns:
- **Controllers**: REST endpoints with `@RestController` and `@RequestMapping`
- **Services**: Business logic with `@Service` annotation
- **Configuration**: External properties via `application.properties` and `@Value`
- **Bean Lifecycle**: Spring manages all component lifecycles (no hanging threads)

### Workflow Implementation:
1. **ResearchController**: REST API receives topic and count parameters, supports both standard JSON and SSE streaming responses
2. **ResearchOrchestrator**: Coordinates async workflow using injected tools and agents, emits SSE events for progress updates
3. **QueryOptimizerAgent**: AI-powered query optimization for better search results
4. **WebSearchTool**: Direct MCP server communication with integrated JSON response parsing
5. **QuickSummaryAgent**: Fast preliminary summarization from search snippets (runs async)
6. **ContentFetcherTool**: Self-contained parallel content fetching with Java HttpClient and JSoup, includes intelligent content cleaning (removes headers, footers, navigation, ads, etc.)
7. **SummarizingAgent**: Comprehensive LLM-powered summarization with updated @V annotation pattern

### Configuration:
- Configuration properties in `application.properties`
- Environment variables override defaults (Docker-friendly)
- System properties supported for runtime config
- Spring `@Value` annotation for property injection

### Code Quality:
- **Consistency**: When fixing errors, check for same pattern across all similar components
- **Clean Architecture**: Eliminate redundant abstraction layers - use direct tool calls when appropriate
- **LangChain4j Integration**: Leverage framework features instead of custom implementations
- **Spring Best Practices**: Follow Spring Boot conventions and patterns