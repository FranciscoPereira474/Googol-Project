# Googol — Web Search Engine

A distributed web search engine inspired by Google, built with **Java 21**, **Spring Boot**, and **Java RMI**. The system crawls web pages, builds an inverted index, and lets users search for pages ranked by relevance (incoming link count).

> Academic project for the **Distributed Systems (SD)** course — BSc in Computer Engineering, University of Coimbra, 2024/2025.

## Features

- **Web Crawler** — parallel downloaders that crawl pages and extract text/links using jsoup
- **Inverted Index** — partitioned index (A–M / N–Z) stored across replicated Storage Barrels
- **Search Engine** — full-text search with results ranked by incoming link count, paginated in groups of 10
- **Web Interface** — Spring Boot + Thymeleaf frontend following the MVC pattern
- **Real-time Stats** — live system statistics (active barrels, top searches, response times) via WebSockets
- **Hacker News Integration** — index URLs from Hacker News top stories matching search terms (REST API)
- **AI-powered Analysis** — contextual analysis of search results using the Gemini API

## Architecture

The system follows a distributed architecture with four main components communicating via Java RMI:

```
World Wide Web
      │
  Downloaders (parallel)  ──RMI──>  Index Storage Barrels (replicated)
      │                                        │
      └── URL Queue                            │ RMI
                                               │
                                        RMI Gateway
                                         │        │
                                    RMI Client   Web Server (Spring Boot)
                                                   │
                                              HTTP/WebSocket
                                                   │
                                               Browsers
```

- **Downloaders** — crawl web pages in parallel, extract text and links, send to Barrels via reliable multicast over RMI
- **Index Storage Barrels** — replicated servers storing the inverted index with failover support
- **RMI Gateway** — entry point for clients, load-balances queries across active Barrels
- **Web Server** — Spring Boot application serving the web interface and integrating with REST APIs

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Web Framework | Spring Boot 3.2 |
| Templating | Thymeleaf |
| HTML Parser | jsoup |
| Communication | Java RMI |
| Real-time | WebSockets |
| Build Tool | Maven |
| External APIs | Hacker News API, Google Gemini API |

## Getting Started

### Prerequisites

- Java 21+
- Maven

### Running the System

Start each component in a separate terminal, from the `java/target/classes` directory:

```bash
# 1. Start the Gateway
java googol.Gateway

# 2. Start a Barrel (port 1100, localhost)
java googol.Barrel 1100 127.0.0.1

# 3. Start a Downloader
java -cp ".:jsoup-1.19.1.jar" googol.Downloader

# 4. Start the Web Server
cd java/
mvn spring-boot:run
```

Open `http://127.0.0.1:8080` in your browser.

### Configuration

Edit `.property_file` to configure the Gateway address:

```
gateway:127.0.0.1:1099
```

For the Gemini API integration, add your API key to `.property_file`:

```
GEMINI_API_KEY:your_api_key_here
```

> ⚠️ Never commit API keys to version control.

## Team

- Francisco Pereira
- Francisco Loureiro
- Gonçalo Borges

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
