# Research Agent Server

A Spring Boot REST API service that leverages LangChain4j and Ollama to provide intelligent web research and content synthesis capabilities.

## Overview

Research Agent Server is a microservice that searches the web for information on any topic, fetches content from multiple sources, and synthesizes the results into a comprehensive summary using AI. Built with Spring Boot 3.2 and LangChain4j, it provides a clean REST API for automated research tasks.

## Features

- 🔍 **Web Search Integration** - Connects to MCP web server for search capabilities
- 🌐 **Parallel Content Fetching** - Efficiently retrieves content from multiple URLs concurrently
- 🤖 **AI-Powered Summarization** - Uses Ollama (llama3.1:latest) for intelligent content synthesis
- 🐳 **Docker Ready** - Containerized deployment with docker-compose
- 🏗️ **Clean Architecture** - Spring Boot + LangChain4j agent-based design
- 📊 **REST API** - Simple HTTP interface for integration

## Architecture

```
┌─────────────────┐
│  REST Client    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────┐
│    Research Agent Server            │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ResearchController         │   │
│  │  (REST API)                 │   │
│  └────────────┬────────────────┘   │
│               │                     │
│  ┌────────────▼────────────────┐   │
│  │  ResearchOrchestrator       │   │
│  │  (Service Layer)            │   │
│  └─┬──────────┬────────────┬──┘   │
│    │          │            │       │
│  ┌─▼─────┐ ┌─▼──────┐  ┌──▼────┐ │
│  │ Web   │ │Content │  │Summa- │ │
│  │Search │ │Fetcher │  │rizing │ │
│  │Tool   │ │Tool    │  │Agent  │ │
│  └───┬───┘ └───┬────┘  └───┬───┘ │
└──────┼─────────┼───────────┼─────┘
       │         │           │
       ▼         ▼           ▼
   ┌────────┐ ┌──────┐  ┌────────┐
   │  MCP   │ │ Web  │  │ Ollama │
   │  Web   │ │Sites │  │(External)
   └────────┘ └──────┘  └────────┘
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

### 3. Test the API

```bash
# Example: Research why sugar is good (3 sources)
curl "http://localhost:8080/api/research?topic=why%20sugar%20is%20good&count=3"

# Example: Research artificial intelligence (5 sources)
curl "http://localhost:8080/api/research?topic=artificial%20intelligence&count=5"
```

## API Documentation

### Research Endpoint

**Endpoint:** `GET /api/research`

**Query Parameters:**
- `topic` (required) - The research topic/question
- `count` (optional, default: 5) - Number of sources to fetch and analyze

**Response:**
```json
{
  "topic": "artificial intelligence",
  "searchResults": [
    {
      "title": "What is Artificial Intelligence?",
      "url": "https://example.com/ai-intro",
      "description": "An introduction to AI..."
    }
  ],
  "contents": [
    "Full content from source 1...",
    "Full content from source 2..."
  ],
  "summary": "Artificial intelligence (AI) is a field of computer science..."
}
```

### Example Requests

```bash
# Basic research request
curl "http://localhost:8080/api/research?topic=climate%20change&count=5"

# Research with fewer sources
curl "http://localhost:8080/api/research?topic=quantum%20computing&count=3"

# Using wget
wget -qO- "http://localhost:8080/api/research?topic=machine%20learning&count=4"
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
│   │   │   │   └── ResearchController.java      # REST API
│   │   │   ├── service/
│   │   │   │   └── ResearchOrchestrator.java    # Business logic
│   │   │   ├── agents/
│   │   │   │   ├── ResearchAgent.java           # Agent interface
│   │   │   │   └── SummarizingAgent.java        # AI summarization
│   │   │   ├── tool/
│   │   │   │   ├── WebSearchTool.java           # Search integration
│   │   │   │   └── ContentFetcherTool.java      # Content retrieval
│   │   │   ├── model/
│   │   │   │   ├── SearchResult.java
│   │   │   │   └── ResearchResult.java
│   │   │   └── config/
│   │   │       └── AgentConfiguration.java      # Spring beans
│   │   └── resources/
│   │       └── application.properties
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