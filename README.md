# Research Agent Server

A Spring Boot REST API service that leverages LangChain4j and Ollama to provide intelligent web research and content synthesis capabilities.

## Overview

Research Agent Server is a microservice that searches the web for information on any topic, fetches content from multiple sources, and synthesizes the results into a comprehensive summary using AI. Built with Spring Boot 3.2 and LangChain4j, it provides a clean REST API for automated research tasks.

## Features

- 🔍 **Web Search Integration** - Connects to MCP web server for search capabilities
- 🧠 **Query Optimization** - AI-powered search query refinement for better results
- 🌐 **Parallel Content Fetching** - Efficiently retrieves content from multiple URLs concurrently
- ⚡ **Quick Summary** - Instant preliminary summaries from search snippets
- 🤖 **AI-Powered Comprehensive Summarization** - Deep content analysis using Ollama (llama3.1:latest)
- 📡 **Real-time Streaming** - Server-Sent Events (SSE) for live progress updates
- 🎨 **Interactive Web UI** - Beautiful, responsive interface with live progress tracking
- 🐳 **Docker Ready** - Containerized deployment with docker-compose
- 🏗️ **Clean Architecture** - Spring Boot + LangChain4j agent-based design
- 📊 **REST API** - Simple HTTP interface for integration

## Architecture

```
┌──────────────────┐         ┌─────────────────┐
│   Web UI (SSE)   │         │  REST Client    │
└────────┬─────────┘         └────────┬────────┘
         │                            │
         └──────────────┬─────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│         Research Agent Server                     │
│                                                   │
│  ┌──────────────────────────────────────────┐    │
│  │  ResearchController                      │    │
│  │  (REST API + SSE Streaming)              │    │
│  └──────────────┬───────────────────────────┘    │
│                 │                                 │
│  ┌──────────────▼───────────────────────────┐    │
│  │  ResearchOrchestrator                    │    │
│  │  (Service Layer + Async Orchestration)   │    │
│  └─┬─────┬─────┬──────────┬──────────┬─────┘    │
│    │     │     │          │          │           │
│  ┌─▼──┐┌─▼──┐┌─▼──────┐┌──▼───────┐┌─▼───────┐ │
│  │Qry ││Web ││Content ││Quick     ││Compreh- │ │
│  │Opt.││Srch││Fetcher ││Summary   ││ensive   │ │
│  │Agt.││Tool││Tool    ││Agent     ││Summary  │ │
│  └─┬──┘└─┬──┘└───┬────┘└──┬───────┘│Agent    │ │
│    │     │       │         │        └─────┬───┘ │
└────┼─────┼───────┼─────────┼──────────────┼─────┘
     │     │       │         │              │
     ▼     ▼       ▼         ▼              ▼
┌────────────┐ ┌──────┐  ┌──────────────────────┐
│   Ollama   │ │ MCP  │  │      Ollama          │
│ (optimize) │ │ Web  │  │  (summarization)     │
└────────────┘ └──┬───┘  └──────────────────────┘
                  │
                  ▼
              ┌──────┐
              │ Web  │
              │Sites │
              └──────┘
```

## Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Docker & Docker Compose** (for containerized deployment)
- **Ollama** with `llama3.1:latest` model installed and running
- **MCP Web Server** (optional if using docker-compose)

## Quick Start

### 1. Install and Start Ollama

```bash
# Install Ollama (if not already installed)
# Visit: https://ollama.ai

# Pull and run llama3.1:latest model
ollama run llama3.1:latest
```

### 2. Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/yourusername/research-agent-server.git
cd research-agent-server

# Start all services (MCP web + Research Agent Server)
docker-compose up --build

# The API will be available at http://localhost:8080
```

### 3. Access the Application

**Option A: Web UI (Recommended)**
```bash
# Open your browser
http://localhost:8080

# Interactive UI with live progress tracking!
```

**Option B: REST API**
```bash
# Standard JSON endpoint
curl "http://localhost:8080/api/research?topic=artificial%20intelligence&count=5"

# Streaming endpoint (SSE)
curl -N "http://localhost:8080/api/research/stream?topic=quantum%20computing&count=3"
```

## API Documentation

### Web UI

**Access:** `http://localhost:8080`

The web UI provides:
- Real-time progress tracking with Server-Sent Events (SSE)
- Collapsible sections for each research stage
- Markdown rendering for summaries
- Interactive result exploration
- Beautiful, responsive design

### REST Endpoints

#### 1. Standard Research Endpoint

**Endpoint:** `GET /api/research`

**Query Parameters:**
- `topic` (required) - The research topic/question
- `count` (optional, default: 5) - Number of sources to fetch and analyze

**Response:**
```json
{
  "topic": "artificial intelligence",
  "optimizedQuery": "artificial intelligence overview applications",
  "searchResults": {
    "results": [
      {
        "title": "What is Artificial Intelligence?",
        "url": "https://example.com/ai-intro",
        "content": "Brief snippet..."
      }
    ]
  },
  "quickSummary": "Based on initial snippets: AI is...",
  "contents": [
    "Full content from source 1...",
    "Full content from source 2..."
  ],
  "comprehensiveSummary": "Comprehensive analysis: Artificial intelligence (AI)..."
}
```

#### 2. Streaming Research Endpoint (SSE)

**Endpoint:** `GET /api/research/stream`

**Query Parameters:**
- `topic` (required) - The research topic/question
- `count` (optional, default: 5) - Number of sources to fetch and analyze

**Response:** Server-Sent Events stream

**Event Types:**
- `step` - Current processing step
- `optimized_query` - Optimized search query
- `search_results` - Search results (JSON)
- `quick_summary` - Preliminary summary from snippets
- `contents_array` - Fetched content (JSON array)
- `comprehensive_summary` - Final comprehensive summary
- `complete` - Research finished
- `error` - Error occurred

**Example (curl):**
```bash
curl -N "http://localhost:8080/api/research/stream?topic=climate%20change&count=3"
```

### Workflow Stages

The research process follows these stages:

1. **Query Optimization** - AI refines the search query for better results
2. **Web Search** - Searches using optimized query via MCP server
3. **Quick Summary** - Generates preliminary summary from search snippets (async)
4. **Content Fetching** - Retrieves full content from URLs in parallel
5. **Comprehensive Summary** - Deep analysis and synthesis of all fetched content

### Example Requests

```bash
# Web UI (recommended)
open http://localhost:8080

# Standard JSON API
curl "http://localhost:8080/api/research?topic=climate%20change&count=5"

# Streaming with live updates
curl -N "http://localhost:8080/api/research/stream?topic=quantum%20computing&count=3"

# Research with fewer sources
curl "http://localhost:8080/api/research?topic=machine%20learning&count=2"
```

## Local Development

### Build and Run Locally (without Docker)

```bash
# Ensure Ollama is running
ollama run llama3.1:latest

# Ensure MCP web server is running on port 9101
# (Start your MCP web server separately)

# Build the project
mvn clean compile

# Run tests
mvn test

# Start the Spring Boot application
mvn spring-boot:run

# The API will be available at http://localhost:8080
```

### Configuration

Configuration is managed via `src/main/resources/application.properties`:

```properties
server.port=8080
ollama.url=http://localhost:11434
ollama.model=llama3.1:latest
mcp.web.url=http://localhost:9101
```

**Environment Variables** (override properties):
- `OLLAMA_BASE_URL` - Ollama server URL
- `OLLAMA_MODEL` - Ollama model name
- `MCP_WEB_URL` - MCP web server URL

## Docker Deployment

### Build Docker Image

```bash
docker build -t research-agent-server:latest .
```

### Run with Docker

```bash
docker run -p 8080:8080 \
  -e OLLAMA_BASE_URL=http://host.docker.internal:11434 \
  -e OLLAMA_MODEL=llama3.1:latest \
  research-agent-server:latest
```

### Docker Compose

The `docker-compose.yml` includes:
- `mcp-web` - MCP web server for search capabilities
- `research-agent-server` - The main application

Ollama runs separately on the host machine and is accessed via `host.docker.internal`.

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f research-agent-server

# Stop services
docker-compose down
```

## Project Structure

```
research-agent-server/
├── src/
│   ├── main/
│   │   ├── java/com/ninickname/summarizer/
│   │   │   ├── ResearchAgentApplication.java    # Spring Boot entry point
│   │   │   ├── controller/
│   │   │   │   └── ResearchController.java      # REST API + SSE streaming
│   │   │   ├── service/
│   │   │   │   └── ResearchOrchestrator.java    # Async workflow orchestration
│   │   │   ├── agents/
│   │   │   │   ├── QueryOptimizerAgent.java     # Query optimization
│   │   │   │   ├── QuickSummaryAgent.java       # Fast snippet summarization
│   │   │   │   └── SummarizingAgent.java        # Comprehensive summarization
│   │   │   ├── tool/
│   │   │   │   ├── WebSearchTool.java           # MCP search integration
│   │   │   │   └── ContentFetcherTool.java      # Parallel content fetching
│   │   │   ├── model/
│   │   │   │   ├── SearxngResult.java           # Search result model
│   │   │   │   ├── SearxngResponse.java         # Search response wrapper
│   │   │   │   └── ResearchResult.java          # Complete research result
│   │   │   └── config/
│   │   │       └── OllamaConfiguration.java     # Spring beans & LLM config
│   │   └── resources/
│   │       ├── application.properties
│   │       └── static/
│   │           └── index.html                   # Interactive web UI
│   └── test/
│       └── java/com/ninickname/summarizer/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── CLAUDE.md                                    # AI assistant instructions
└── README.md
```

## Technology Stack

- **Spring Boot 3.2** - Application framework
- **LangChain4j 0.34.0** - AI orchestration and agent framework
- **Ollama** - Local LLM inference (llama3.1:latest)
- **JSoup** - HTML parsing and content extraction
- **Apache HttpClient 5** - HTTP communication
- **Maven** - Build and dependency management
- **Docker** - Containerization

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [LangChain4j](https://github.com/langchain4j/langchain4j) - Java framework for LLM applications
- [Ollama](https://ollama.ai) - Local LLM inference engine
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check existing issues for solutions

---

**Built with ❤️ using Spring Boot and LangChain4j**