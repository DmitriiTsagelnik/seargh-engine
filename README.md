# Search Engine

A full-stack search engine application built with **Java and Spring Boot**.

The application crawls configured websites, analyzes their content, builds a searchable index and provides relevant search results through both a **web interface** and a **REST API**.

The project demonstrates backend development, database integration, multithreaded web crawling, text processing, search indexing and containerized deployment.

---

## Features

* Website indexing
* Multithreaded website crawling
* HTML content parsing
* Russian language lemmatization
* Search by query
* Search result relevance ranking
* Website-specific search
* Indexing individual pages
* Indexing status monitoring
* REST API
* Swagger / OpenAPI documentation
* Web interface
* MySQL database
* Docker and Docker Compose support

---

## Web Interface

The application provides a web interface for interacting with the search engine.

The interface allows users to:

* start and stop website indexing;
* view the indexing status of configured websites;
* index individual pages;
* view indexing statistics;
* perform searches;
* select a specific website for searching;
* view search results with page title, URL and relevance.

The frontend communicates with the backend through the REST API.

When running locally, the web interface is available at:

```text
http://localhost:8080/
```

### Interface

![Search Engine Web Interface](docs/screenshots/web-interface.png)

---

## Swagger / OpenAPI

The REST API is documented using **Swagger / OpenAPI**.

Swagger UI provides an interactive interface for viewing and testing all available API endpoints.

When the application is running:

```text
http://localhost:8080/swagger-ui/index.html
```

### Swagger UI

![Swagger UI](docs/screenshots/swagger.png)

---

## Technologies

### Backend

* Java 23
* Spring Boot 3
* Spring MVC
* Spring Data JPA
* Hibernate
* REST API

### Database

* MySQL 8

### Libraries

* Jsoup — HTML parsing and website crawling
* Apache Lucene Morphology — Russian language lemmatization
* Lombok
* Springdoc OpenAPI — Swagger documentation
* Thymeleaf — web interface

### Tools

* Maven
* Git
* GitHub
* IntelliJ IDEA
* Docker
* Docker Compose

---

## Application Architecture

The project follows a layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

### Main components

**Controllers**

Handle HTTP requests and provide the REST API.

**Services**

Contain the main business logic of indexing, text processing and searching.

**Repositories**

Provide database access using Spring Data JPA.

**Entities**

Represent database tables and relationships.

**DTO**

Transfer data between the application layers and API.

---

## Indexing Process

The indexing process works as follows:

```text
Website
   ↓
Web Crawler
   ↓
HTML Parsing
   ↓
Text Extraction
   ↓
Lemmatization
   ↓
Search Index
   ↓
MySQL
```

1. User starts indexing through the web interface or REST API.
2. The application crawls the configured website.
3. Pages are processed using multithreaded crawling.
4. HTML content is extracted and cleaned.
5. Words are converted into their normal forms (lemmas).
6. Lemmas and page information are stored in MySQL.
7. The resulting data is used for search.

The crawler uses concurrent processing to improve indexing performance on websites with multiple pages.

---

## Search Process

The search process consists of several stages:

```text
Search Query
     ↓
Lemmatization
     ↓
Lemma Selection
     ↓
Page Matching
     ↓
Relevance Calculation
     ↓
Sorted Results
```

1. User enters a search query.
2. The query is converted into normalized lemmas.
3. Common lemmas are filtered when necessary.
4. Pages containing the required lemmas are found.
5. Matching pages are ranked according to relevance.
6. Search results are returned through the API and displayed in the web interface.

---

## Database Structure

The application uses MySQL for persistent storage.

Main entities:

### Site

Stores information about indexed websites:

* URL
* name
* indexing status
* status time
* error information

### Page

Stores indexed website pages:

* website
* page path
* HTTP response code
* page content

### Lemma

Stores normalized words extracted from page content and their frequency.

### Index

Connects pages with lemmas and stores ranking information used during search.

Simplified relationship:

```text
Site
 ├── Page
 │    └── Index ─── Lemma
 │
 └── ...
```

---

## REST API

### Start indexing

```http
POST /api/startIndexing
```

Starts indexing of all configured websites.

### Stop indexing

```http
GET /api/stopIndexing
```

Stops the current indexing process.

### Index single page

```http
POST /api/indexPage
```

Adds a single page to the search index.

### Search

```http
GET /api/search?query=java
```

Searches indexed pages by query.

The API also supports selecting a specific website and pagination of search results.

For the complete API specification and available parameters, use Swagger UI.

---

## Configuration

Application settings are stored in `application.yaml`.

Example:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/search_engine
    username: root
    password: your_password

indexing-settings:
  user-agent: Mozilla/5.0 (compatible; SearchBot/1.0)
  referrer: http://google.com
  max-depth: 4

sites:
  - url: https://www.aviasales.by/
    name: Дешевые авиабилеты

  - url: https://gomelkino.by/
    name: Лучшие новинки гомельского кино
```

---

# Running Locally

## Requirements

* Java 23
* Maven
* MySQL 8

### 1. Clone the repository

```bash
git clone https://github.com/DmitriiTsagelnik/search-engine.git
cd search-engine
```

### 2. Create the database

```sql
CREATE DATABASE search_engine;
```

### 3. Configure the database

Update the database credentials in:

```text
src/main/resources/application.yaml
```

### 4. Build the application

```bash
mvn clean package
```

### 5. Run the application

```bash
mvn spring-boot:run
```

The application will be available at:

```text
http://localhost:8080/
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# Running with Docker

The project includes Docker configuration for running the application together with MySQL.

### Requirements

* Docker
* Docker Compose

### Start the application

First build the application:

```bash
mvn clean package
```

Then run:

```bash
docker compose up --build
```

Docker Compose starts:

```text
┌─────────────────────┐
│   Search Engine     │
│   Spring Boot       │
│   :8080             │
└──────────┬──────────┘
           │
           │
┌──────────▼──────────┐
│       MySQL         │
│       :3306         │
└─────────────────────┘
```

The application will be available at:

```text
http://localhost:8080/
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

MySQL data is persisted using a Docker volume.

To stop the application:

```bash
docker compose down
```

---

## Project Structure

```text
search-engine/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── searchengine/
│   │   │       ├── config/
│   │   │       ├── controllers/
│   │   │       ├── dto/
│   │   │       ├── model/
│   │   │       ├── repositories/
│   │   │       └── services/
│   │   │
│   │   └── resources/
│   │       ├── static/
│   │       ├── templates/
│   │       └── application.yaml
│   │
│   └── test/
│
├── Dockerfile
├── docker-compose.yaml
├── pom.xml
└── README.md
```

---

## Project Highlights

This project demonstrates practical experience with:

* Java backend development
* Spring Boot
* REST API design
* Spring Data JPA
* Hibernate
* MySQL
* multithreading and concurrent processing
* web crawling
* HTML parsing
* text normalization and lemmatization
* search indexing
* relevance ranking
* layered application architecture
* Swagger / OpenAPI
* Docker
* Docker Compose
* Git and GitHub

---

## Project Status

The project is a working search engine application with:

* web interface;
* REST API;
* Swagger documentation;
* website indexing;
* multithreaded crawling;
* Russian language lemmatization;
* relevance-based search;
* MySQL persistence;
* Docker Compose deployment.

---

## Author

**Dmitrii Tsagelnik**

GitHub:

https://github.com/DmitriiTsagelnik
