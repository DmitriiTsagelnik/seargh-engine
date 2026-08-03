# Search Engine

Backend application for indexing websites and searching information by content.

The application crawls websites, analyzes page content, creates a search index and provides search functionality through a REST API.

## Features

- Website indexing
- Multithreaded page crawling
- HTML content parsing
- Russian language lemmatization
- Search by query relevance
- REST API for indexing and searching
- Data storage in MySQL database

## Technologies

### Backend
- Java 23
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- REST API

### Database
- MySQL

### Libraries
- Jsoup — HTML parsing
- Apache Lucene Morphology — Russian word lemmatization

### Tools
- Maven
- Git
- IntelliJ IDEA

## Application Architecture

The project follows a layered architecture:


Controller
|
Service
|
Repository
|
Database


Main components:

- **Controllers** — handle HTTP requests and provide REST API endpoints
- **Services** — contain business logic
- **Repositories** — database interaction using Spring Data JPA
- **Entities** — represent database tables

## Database Structure

The application uses the following entities:

- `Site` — stores information about indexed websites
- `Page` — stores indexed pages
- `Lemma` — stores normalized words from page content
- `Index` — connects pages with lemmas and stores ranking information

## Indexing Process

1. User starts website indexing through REST API.
2. Application crawls website pages.
3. HTML content is extracted and cleaned.
4. Words are converted into their normal forms (lemmas).
5. Index data is stored in MySQL database.

## Search Process

1. User sends a search query.
2. Query text is converted into lemmas.
3. Application finds pages containing matching lemmas.
4. Results are ranked by relevance.
5. Matching pages are returned through REST API.

## REST API

### Start indexing


POST /api/startIndexing


Starts indexing of configured websites.

---

### Stop indexing


GET /api/stopIndexing


Stops the current indexing process.

---

### Index single page


POST /api/indexPage


Adds a single page to the search index.

---

### Search


GET /api/search


Searches indexed pages by query.

Example:


GET /api/search?query=java


## Configuration

Before running the application, configure:

- MySQL database connection
- Websites for indexing

Example:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/search_engine
    username: root
    password: your_password
How to Run
Requirements
Java 23+
Maven
MySQL 8+
Steps
Clone repository:
git clone https://github.com/DmitriiTsagelnik/search-engine.git
Configure database connection.
Create database:
CREATE DATABASE search_engine;
Run application:
mvn spring-boot:run
Project Status

Completed learning project demonstrating backend development with Spring Boot, database integration, REST API design and search engine implementation.

Author

Dmitrii Tsagelnik

GitHub:
https://github.com/DmitriiTsagelnik
