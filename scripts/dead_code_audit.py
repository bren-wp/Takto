#!/usr/bin/env python3
"""Conservative static dead-code audit for Takto production Kotlin sources.

The audit deliberately avoids deleting or failing framework entry points reached from
Android manifests, Compose runtime, reflection or callbacks. Strict mode fails only
on private Kotlin symbols that are provably referenced once in their own source tree.
Top-level candidates and TODO/FIXME markers are reported for manual review.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "app/src/main/java"
MANIFEST_PATH = ROOT / "app/src/main/AndroidManifest.xml"
KOTLIN_FILES = sorted(SOURCE_ROOT.rglob("*.kt")) if SOURCE_ROOT.exists() else []

contents = {path: path.read_text(encoding="utf-8") for path in KOTLIN_FILES}
all_text = "\n".join(contents.values())
manifest_text = MANIFEST_PATH.read_text(encoding="utf-8") if MANIFEST_PATH.exists() else ""
manifest_components = set(
    re.findall(r'android:name="(?:\.[A-Za-z0-9_$.]*\.)?([A-Z][A-Za-z0-9_]*)"', manifest_text)
)

dead_private = []
top_level_candidates = []
markers = []

for path, source in contents.items():
    rel = path.relative_to(ROOT).as_posix()

    for line_no, line in enumerate(source.splitlines(), 1):
        if re.search(r"\b(?:TODO|FIXME)\b", line):
            markers.append({"path": rel, "line": line_no, "text": line.strip()})

    private_functions = set(
        re.findall(
            r"\bprivate\s+(?:suspend\s+)?fun\s+(?:[A-Za-z_][A-Za-z0-9_]*\.)?([A-Za-z_][A-Za-z0-9_]*)\s*\(",
            source,
        )
    )
    private_properties = set(
        re.findall(r"\bprivate\s+(?:const\s+)?(?:val|var)\s+([A-Za-z_][A-Za-z0-9_]*)", source)
    )

    for name in sorted(private_functions | private_properties):
        count = len(re.findall(rf"\b{re.escape(name)}\b", all_text))
        if count == 1:
            dead_private.append({"path": rel, "symbol": name})

    top_level = set(
        re.findall(
            r"^(?:data\s+class|enum\s+class|class|object)\s+([A-Z][A-Za-z0-9_]*)",
            source,
            flags=re.MULTILINE,
        )
    )
    for name in sorted(top_level):
        if name in {"MainActivity", "TaktoApplication"} or name in manifest_components:
            continue
        count = len(re.findall(rf"\b{re.escape(name)}\b", all_text))
        if count == 1:
            top_level_candidates.append({"path": rel, "symbol": name})

report = {
    "scanned_kotlin_files": len(KOTLIN_FILES),
    "dead_private_kotlin": dead_private,
    "top_level_review_candidates": top_level_candidates,
    "todo_fixme_markers": markers,
    "manifest_components_ignored": sorted(manifest_components),
}
print(json.dumps(report, indent=2, ensure_ascii=False))

parser = argparse.ArgumentParser()
parser.add_argument("--strict", action="store_true")
args = parser.parse_args()

if args.strict and (dead_private or top_level_candidates or markers):
    reasons = []
    if dead_private:
        reasons.append("private Kotlin symbols referenced only at declaration")
    if top_level_candidates:
        reasons.append("top-level Kotlin declarations with no source references")
    if markers:
        reasons.append("TODO/FIXME markers in production Kotlin")
    raise SystemExit("Dead-code audit failed: " + "; ".join(reasons) + ".")
