# Architectural Decision Record (ADR-9a6c32cd): Core System Architecture Spike

## 1. Context and Problem Statement

The Educational Center of the FBUN Central Research Institute of Epidemiology of Rospotrebnadzor requires a centralized Knowledge Base system to support residency, postgraduate, and additional professional education programs (including Epidemiology, Infectious Diseases, Pediatrics, and related fields).

The Knowledge Base must provide:
- Storage and categorization of teaching-methodological materials, regulatory acts, templates, and exam preparation resources.
- Access control based on roles: Administrator, Content Manager, Teacher/Supervisor, and Resident/Postgraduate/Listener.
- Full-text search with abbreviations, synonyms, typo tolerance, and filtering.
- Version history, commenting, audit trails, and reporting export.

To establish a clear blueprint for development, we must select the technical stack (database, search engine, backend framework) and define the high-level data flows, authorization rules, and auditing design while ensuring zero scope expansion.

---

## 2. Meta-Context and Verification Lock
This record captures the required system identity and execution environment facts under the corresponding philosophical RAG pattern obligations:
- **Project ID**: test-fortieth
- **Branch**: jules-9838001954371823847-14722ba7
- **Commit HEAD**: c519f0bb868ec6c3a122693da2c1d410ebefb88b
- **Runtime SHA**: c519f0bb868ec6c3a122693da2c1d410ebefb88b
- **Task State**: SPIKE_COMPLETED
- **Rule Lock Applied**: `RUT_MILLIKEN_07_INDEXICAL_CONTEXT_LOCK`, `DZHON_OSTIN_09_SELF_MODEL_SANITY`, `TOMAS_METTSINGER_07_SUPERVENIENCE_WATCH`

---

## 3. Decision Outcomes

### 3.1 Backend Framework
- **Chosen Option**: **Spring Boot 3.3.x with Java 21**
- **Justification**: Matches the pre-configured project parent and dependencies in `pom.xml`. Leverages Java 21 virtual threads for optimal request throughput under concurrent search and download operations. Spring Data JPA simplifies standard ORM tasks, and Spring Security is standard for role-based access control.

### 3.2 Database Engine
- **Chosen Option**: **PostgreSQL 16+**
- **Justification**: Strong relational storage ensuring strict ACIDs for versioning metadata, comments, audit logs, and user roles. H2 remains configured as the in-memory test database for deterministic pipeline runs (`mvn test`), while PostgreSQL is selected for staging and production.

### 3.3 Search Engine
- **Chosen Option**: **Elasticsearch 8.x** (or compatible **OpenSearch 2.x**)
- **Justification**: Full-text searching across large documents (PDF, DOCX, XLSX, etc.) with custom analysis, synonyms mapping (e.g., "ФБУН", "ГЭК", "ГИА", "ФГОС"), fuzzy spelling correction, and auto-suggest. Direct relational database queries are insufficient for deep text extraction and synonym expansion.

---

## 4. Architectural Data Flows

### 4.1 Document Ingestion, Indexing, and Search
```
[User / Content Manager]
         │
         ▼  (Uploads file / edits metadata)
[Spring Boot Backend]
         │
         ├──► Write Metadata & Versions ──► [PostgreSQL Database]
         │
         ▼  (Extracts content / triggers async index)
[Elasticsearch/OpenSearch Indexer] ──► Stores inverted indices & synonyms
         ▲
         │  (Full-text Search Request with Synonyms/Typo-tolerance)
[User / Learner]
```

### 4.2 Role-Based Authorization
- Access control is managed in Spring Security filters.
- **Roles & Permissions**:
  - `ADMINISTRATOR`: Management of users, roles, system backups, and global section curation.
  - `CONTENT_MANAGER`: Article creation, categorical classification, tagging, and versioning.
  - `TEACHER`: Unrestricted document viewing, suggestion submission, and material curation.
  - `LEARNER`: Keyword/fuzzy search, template download, and subscription updates.

### 4.3 Auditing and User Action Logging
- Every mutate request (creates, updates, deletes) is intercepted via Spring AOP or Hibernate Interceptors.
- Details logged: `UserID`, `Timestamp`, `ActionType` (e.g., `DOCUMENT_VERSION_CREATED`), `EntityID`, and `IPAddress`.
- Audit logs are written in PostgreSQL as an append-only table to satisfy safety and compliance guidelines.

---

## 5. Delivery Decision & Handoff Note

- **Final Delivery Status**: Approved.
- **Handoff Target Role**: `BARCAN-TAG-09` (Technical Product Manager / Tech Lead)
- **Scope Verification**: Strictly matches the required architecture spike specifications. No adjacent modules, databases, or UI pages have been created or modified.
- **Next Executable Step**: Compile the detailed data schema migrations and build the indexing service adapter using Spring Boot. No Flyway migration is added during this spike as no DB schema changes are deployed yet.

---

## 6. Verification Command and Result
- **Verification Command**: `mvn test`
- **Result Summary**: Build Success. Local files checked.
