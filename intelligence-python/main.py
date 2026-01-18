#!/usr/bin/env python3
"""
SignalGraph intelligence (Python) — skeleton

WHY: A simple, dependency-free CLI that demonstrates how the intelligence layer will be invoked.
It performs deterministic tokenization/preview of files in `data/` and prints JSON-like structured output.

This module uses only Python standard library to avoid external dependencies in V1. Later we will
introduce focused libraries (spaCy, scikit-learn) behind well-documented interfaces.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
from typing import List, Dict


def list_files(path: Path) -> List[Path]:
    """Deterministic file discovery: list files sorted by name.

    WHY: Sorting ensures the pipeline is deterministic given the same directory contents.
    """
    if not path.exists():
        return []
    files = [p for p in path.iterdir() if p.is_file()]
    return sorted(files, key=lambda p: p.name)


def preview_file(p: Path, length: int = 200) -> str:
    """Read a deterministic preview of the file contents.

    WHY: Provide provenance-friendly previews for explainability.
    """
    try:
        text = p.read_text(encoding="utf-8")
        return text[:length].replace("\n", " ")
    except Exception as e:
        return f"<error reading file: {e}>"


def process_file(p: Path) -> Dict:
    """Process a single file and return structured representation.

    For V1 this function returns placeholders for entities and relations. Each returned
    entity/relation must include provenance (file, span, rule id) in future iterations.
    """
    preview = preview_file(p)
    return {
        "file": str(p),
        "preview": preview,
        "entities": [],  # placeholder: [{"text": "Alice", "type": "PERSON", "span": [start,end], "rule_id": "rule-1", "confidence": 1.0}]
        "relations": [],  # placeholder
        "provenance": {"source": str(p)},
    }


def process_files(paths: List[Path]) -> Dict:
    """Process files and return a minimal structured representation.

    This is a placeholder for where tokenization, entity extraction, and relation extraction
    would be implemented.
    """
    results = {"files": []}
    for p in paths:
        results["files"].append(process_file(p))
    return results


def main():
    parser = argparse.ArgumentParser(description="SignalGraph intelligence (Python) — skeleton")
    parser.add_argument("--data-dir", default="data/", help="Path to local data directory")
    parser.add_argument("--file", default=None, help="Path to a single file to process")
    parser.add_argument("--output", default=None, help="Write JSON output to file")
    args = parser.parse_args()

    if args.file:
        p = Path(args.file)
        if not p.exists():
            print(f"[intelligence] error: file not found: {p}")
            raise SystemExit(2)
        print(f"[intelligence] processing single file: {p}")
        results = {"files": [process_file(p)]}
    else:
        data_path = Path(args.data_dir)
        files = list_files(data_path)
        print(f"[intelligence] discovered {len(files)} files in {data_path}")
        results = process_files(files)

    out_json = json.dumps(results, indent=2)

    if args.output:
        outpath = Path(args.output)
        outpath.write_text(out_json, encoding="utf-8")
        print(f"[intelligence] written output to {outpath}")
    else:
        print(out_json)


if __name__ == "__main__":
    main()
