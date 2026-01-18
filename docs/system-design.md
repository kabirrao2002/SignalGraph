# SignalGraph — System Design (V1)

Purpose
- Convert local, public text files into a structured, explainable knowledge graph and surface simple insights via a CLI.

Goals and constraints
- Zero-cost, local-only execution
- No personal data
- Java for orchestration/CLI, Python for intelligence
- Deterministic, explainable pipelines before any ML-heavy components

High-level components
1. Sources (local files under `/data`) — plain text, PDFs (future), HTML
2. Orchestrator (Java)
   - Job scheduling, local file discovery, deterministic preprocessing
   - Provides a CLI for running ingestion and analysis workflows
   - Invokes Python intelligence via subprocess or local REST in later sprints
3. Intelligence (Python)
   - Deterministic preprocessing (tokenization, normalization)
   - Entity extraction: combination of rule-based heuristics and lightweight ML models
   - Relation extraction: syntactic patterns + co-occurrence heuristics
   - Graph builder: NetworkX in-memory graph with export to JSON
   - Explainability: provenance for every entity/relation (source file, span, extraction rule)
4. Local Store
   - V1: JSON + NetworkX in-memory persistence; export/import to disk
   - Future: optional Neo4j docker-compose for heavier experiments
5. Insights Engine
   - Graph-based metrics: centrality, communities, frequent motifs
   - CLI commands to run queries and produce CSV/JSON outputs

Data flow (deterministic)
1. Orchestrator scans `data/` for new files and creates an ingestion job (file path + checksum)
2. Orchestrator performs lightweight text extraction (plain text files only in V1)
3. Orchestrator calls Python intelligence module with the extracted text (via subprocess CLI)
4. Intelligence returns structured entities and relations with provenance
5. Orchestrator persists the graph export to `data/graphs/` and registers an insight job
6. Insights CLI reads the stored graph export and runs deterministic analyses

Why Java for orchestration?
- Strong typing and mature scheduling libraries
- Familiarity in backend engineering interviews
- A simple Java CLI shows system-design ability without heavy frameworks

Why Python for intelligence?
- Rich NLP ecosystem (spaCy, scikit-learn) for future extensions
- Easier experimentation and explainability tooling (SHAP/LIME) later

Explainability strategy
- Prefer rule-based/extracted features with clear provenance
- Attach provenance metadata to each graph element: source file, character span, rule id, confidence score
- Use lightweight models only when rule coverage is insufficient; always retain deterministic fallback

Determinism and testing
- All steps produce stable outputs given the same inputs (file checksum + deterministic tokenization)
- Unit tests for extraction rules and graph logic
- Fixture-based integration tests with sample files in `/data/sample/`

CLI contract (initial)
- `java -jar orchestrator.jar ingest --path data/` — discover and ingest files
- `python intelligence-python/main.py --file data/foo.txt --output out.json` — process a single file and print entities
- `scripts/insights.sh` — run basic graph queries and export CSV

Tradeoffs and next steps
- V1 favors simplicity and auditability over scale
- If scale/complex queries are required, add Neo4j in future sprints (docker-compose)
- Introduce lightweight REST between Java and Python only if subprocess approach becomes limiting

Appendix: storage schema (V1 JSON)
- Graph export format (JSON):
  - nodes: [ { id, label, type, provenance: { file, span, rule } } ]
  - edges: [ { source, target, label, provenance } ]
  - metadata: { created_at, source_files }

This document is intentionally practical: the first iterations favor a reproducible, reviewable codebase that demonstrates system thinking, deterministic pipelines, and explainability.
