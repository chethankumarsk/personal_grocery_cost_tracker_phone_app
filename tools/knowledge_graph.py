#!/usr/bin/env python3
"""Query the Grocery Ledger project knowledge graph.

Examples:
    python tools/knowledge_graph.py autocomplete
    python tools/knowledge_graph.py --id data.csv
    python tools/knowledge_graph.py --status pending
    python tools/knowledge_graph.py --type feature
    python tools/knowledge_graph.py --json update
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GRAPH_PATH = ROOT / "docs" / "knowledge-graph.json"


def load_graph() -> dict:
    with GRAPH_PATH.open(encoding="utf-8") as handle:
        return json.load(handle)


def searchable_text(node: dict) -> str:
    return " ".join(
        str(node.get(key, ""))
        for key in ("id", "type", "label", "status", "path", "summary")
    ).lower()


def matches(node: dict, args: argparse.Namespace) -> bool:
    if args.id and node.get("id") != args.id:
        return False
    if args.status and node.get("status") != args.status:
        return False
    if args.type and node.get("type") != args.type:
        return False
    if args.query and args.query.lower() not in searchable_text(node):
        return False
    return True


def match_priority(node: dict, query: str) -> int:
    if not query:
        return 0
    node_id = str(node.get("id", "")).lower()
    label = str(node.get("label", "")).lower()
    if query == node_id or query == label:
        return 0
    if query in node_id or query in label:
        return 1
    if query in str(node.get("type", "")).lower() or query in str(node.get("path", "")).lower():
        return 2
    return 3


def node_map(graph: dict) -> dict[str, dict]:
    return {node["id"]: node for node in graph["nodes"]}


def print_summary(graph: dict) -> None:
    nodes = graph["nodes"]
    edges = graph["edges"]
    project = graph["project"]
    print(f"{project['name']} {project['version']} ({project['package']})")
    print(f"Status: {project['status']}")
    print(f"Graph: {len(nodes)} nodes, {len(edges)} edges")
    print()

    by_type: dict[str, int] = {}
    by_status: dict[str, int] = {}
    for node in nodes:
        by_type[node["type"]] = by_type.get(node["type"], 0) + 1
        by_status[node.get("status", "unknown")] = by_status.get(node.get("status", "unknown"), 0) + 1

    print("Types: " + ", ".join(f"{key}={value}" for key, value in sorted(by_type.items())))
    print("Statuses: " + ", ".join(f"{key}={value}" for key, value in sorted(by_status.items())))
    print()
    print("Usage:")
    print("  python tools/knowledge_graph.py <text>")
    print("  python tools/knowledge_graph.py --id <node-id>")
    print("  python tools/knowledge_graph.py --status pending|partial|done")
    print("  python tools/knowledge_graph.py --type feature|requirement_group|test")
    print("  python tools/knowledge_graph.py --json <text>")


def print_node(node: dict, graph: dict, show_edges: bool = True) -> None:
    print(f"[{node.get('status', '?')}] {node['id']}")
    print(f"  {node.get('type', '?')}: {node.get('label', '')}")
    if node.get("path"):
        print(f"  path: {node['path']}")
    if node.get("summary"):
        print(f"  {node['summary']}")
    if not show_edges:
        return

    by_id = node_map(graph)
    outgoing = [edge for edge in graph["edges"] if edge["from"] == node["id"]]
    incoming = [edge for edge in graph["edges"] if edge["to"] == node["id"]]
    if outgoing:
        print("  -> " + "; ".join(
            f"{edge['type']} {edge['to']} ({by_id.get(edge['to'], {}).get('status', '?')})"
            for edge in outgoing
        ))
    if incoming:
        print("  <- " + "; ".join(
            f"{edge['type']} {edge['from']} ({by_id.get(edge['from'], {}).get('status', '?')})"
            for edge in incoming
        ))
    print()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Query docs/knowledge-graph.json without reading the whole JSON.",
    )
    parser.add_argument("query", nargs="*", help="case-insensitive text search")
    parser.add_argument("--id", help="exact node ID")
    parser.add_argument("--status", help="done, partial, pending, or deferred")
    parser.add_argument("--type", help="application, module, feature, requirement_group, ...")
    parser.add_argument("--json", action="store_true", help="emit matching nodes/edges as JSON")
    args = parser.parse_args()
    args.query = " ".join(args.query).strip().lower()

    graph = load_graph()

    if not any((args.query, args.id, args.status, args.type)):
        print_summary(graph)
        return 0

    selected = [node for node in graph["nodes"] if matches(node, args)]
    selected.sort(key=lambda node: (match_priority(node, args.query), node["id"]))
    if not selected:
        print("No matching nodes.", file=sys.stderr)
        return 1

    if args.json:
        ids = {node["id"] for node in selected}
        output = {
            "project": graph["project"],
            "nodes": selected,
            "edges": [
                edge for edge in graph["edges"]
                if edge["from"] in ids or edge["to"] in ids
            ],
        }
        print(json.dumps(output, indent=2, ensure_ascii=False))
        return 0

    show_edges = len(selected) <= 6
    for node in selected:
        print_node(node, graph, show_edges=show_edges)
    if not show_edges:
        print(f"{len(selected)} matches; narrow the query or use --id for edges.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
