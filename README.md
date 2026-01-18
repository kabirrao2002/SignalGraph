# SignalGraph

SignalGraph is a local-first, zero-cost research project that ingests unstructured public text and converts it into a structured, explainable knowledge graph that surfaces deterministic insights via a CLI.

This repository is intentionally scoped and structured to demonstrate system design, backend engineering, and explainable ML practices suitable for senior engineering review.

Core principles
- Local-first & privacy-preserving: all data and computation run locally; no external paid services.
- Explainable & deterministic: prefer rule-based, provenance-rich processing before any black-box models.
- Clear separation of concerns: Java for orchestration/CLI, Python for NLP/graph logic.
- Production-oriented layout: minimal dependencies, testable components, and documented tradeoffs.

V1 goals
1. Ingest multiple local text documents
2. Extract entities (people, orgs, technologies) using explainable rules
3. Extract relationships between entities (syntactic/co-occurrence heuristics)
4. Build a local knowledge graph (NetworkX/JSON in V1)
5. Run simple graph-based insights
6. Expose results via a deterministic CLI

Repository layout (must follow exactly)

signalgraph/
├── README.md
├── docs/system-design.md
├── orchestrator-java/
├── intelligence-python/
├── data/
└── scripts/

What is in this scaffold
- `orchestrator-java/` — a minimal Java CLI skeleton (Maven) that demonstrates orchestration and deterministic file discovery.
- `intelligence-python/` — a minimal Python CLI skeleton that discovers files and prints structured previews. This is the intelligence entry point for NLP and graph construction.
- `docs/system-design.md` — concise system design and tradeoffs for V1.
- `data/` — place sample text files here for ingestion.
- `scripts/` — utility scripts for running common workflows.

Quickstart (macOS / Linux)

1) Build the Java orchestrator (Maven required)

```bash
cd signalgraph
mvn -f orchestrator-java clean package -q
```

2) Run the Java orchestrator skeleton (prints usage)

```bash
java -cp orchestrator-java/target/orchestrator-java-1.0-SNAPSHOT.jar com.signalgraph.orchestrator.Main
```

3) Run the intelligence Python skeleton (create venv if desired)

```bash
cd signalgraph
python3 -m venv .venv
source .venv/bin/activate
python intelligence-python/main.py --help
```

4) Example: discover files under `data/` with the Python intelligence skeleton

```bash
python intelligence-python/main.py --data-dir data/ --output out.json
```

Notes and developer guidance
- Keep the orchestrator dependency-free in V1: explicit subprocess calls to Python are acceptable and easy to audit.
- All extractors must attach provenance: source file, character span, rule id, confidence score.
- Determinism: directory listing, tokenization, and all rule evaluations should be deterministic. Include file checksums in job metadata.
- No personal data: use only public/sample documents in `data/`.

Next steps (after you confirm)
- Implement deterministic ingestion in Java (file discovery, checksums, subprocess invocation of Python intelligence).
- Implement rule-based entity and relation extraction in Python plus a lightweight graph writer (NetworkX -> JSON).
- Add unit tests and fixture sample files under `data/sample/` to demonstrate pipeline reproducibility.

Contributing
- This is a personal research project scaffold. If you work with others, follow the folder layout exactly and document any deviations in `docs/system-design.md`.

License
- Include a permissive license of your choice in the project root when ready (e.g., MIT). For now, this scaffold contains no license file.

If this README looks good I will proceed to improve `docs/system-design.md` or implement the Java ingestion flow — tell me which you prefer.
