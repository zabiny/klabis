#!/usr/bin/env python3
"""
Safely update the three dynamic regions of implementation-status.html:
  1. <script id="data" type="application/json">…</script>  — kanban data
  2. "SNAPSHOT · <date>" chip in the header                — timestamp
  3. "Mapa <N otevřených>" in the subtitle                 — total count

The script preserves everything else so the user's CSS/JS tweaks stay intact.
It exits with non-zero status if any of the three anchor patterns is missing
(meaning the template diverged too much and needs manual attention).

Usage:
  update_board.py --html <path> --data <json-file> --snapshot "YYYY-MM-DD HH:MM TZ" --total <N>
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path


SCRIPT_RE = re.compile(
    r'(<script id="data" type="application/json">)(.*?)(</script>)',
    re.DOTALL,
)
SNAPSHOT_RE = re.compile(
    r'(<span class="live">SNAPSHOT\s*·\s*)([^<]*?)(</span>)',
)
TOTAL_RE = re.compile(
    r'(Mapa\s+<strong>)(\d+\s+otevřených)(</strong>)',
)


def fail(msg: str) -> None:
    print(f"update_board.py: {msg}", file=sys.stderr)
    sys.exit(1)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--html", required=True, help="Path to implementation-status.html")
    ap.add_argument("--data", required=True, help="Path to JSON file with issues array")
    ap.add_argument("--snapshot", required=True, help='Timestamp string, e.g. "2026-04-19 05:45 CEST"')
    ap.add_argument("--total", required=True, type=int, help="Total number of issues shown")
    args = ap.parse_args()

    html_path = Path(args.html)
    data_path = Path(args.data)

    if not html_path.exists():
        fail(f"HTML file not found: {html_path}")
    if not data_path.exists():
        fail(f"Data file not found: {data_path}")

    # Validate the JSON payload — fail loudly if it is not a proper array.
    try:
        payload = json.loads(data_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(f"Data file is not valid JSON: {exc}")
    if not isinstance(payload, list):
        fail("Data file must be a JSON array of issue objects")

    # Pretty-format one object per line to keep git diffs readable.
    lines = ["["]
    for i, item in enumerate(payload):
        comma = "," if i < len(payload) - 1 else ""
        lines.append(json.dumps(item, ensure_ascii=False, separators=(",", ":")) + comma)
    lines.append("]")
    formatted = "\n".join(lines)

    html = html_path.read_text(encoding="utf-8")

    # 1. Replace the data block
    def replace_script(m: re.Match[str]) -> str:
        return f"{m.group(1)}\n{formatted}\n{m.group(3)}"

    new_html, n_script = SCRIPT_RE.subn(replace_script, html, count=1)
    if n_script == 0:
        fail('Anchor not found: <script id="data" type="application/json">')

    # 2. Replace snapshot timestamp
    new_html, n_snap = SNAPSHOT_RE.subn(
        lambda m: f"{m.group(1)}{args.snapshot}{m.group(3)}",
        new_html,
        count=1,
    )
    if n_snap == 0:
        fail('Anchor not found: <span class="live">SNAPSHOT · …</span>')

    # 3. Replace total count in subtitle
    new_html, n_total = TOTAL_RE.subn(
        lambda m: f"{m.group(1)}{args.total} otevřených{m.group(3)}",
        new_html,
        count=1,
    )
    if n_total == 0:
        fail('Anchor not found: "Mapa <strong>N otevřených</strong>"')

    html_path.write_text(new_html, encoding="utf-8")
    print(
        f"OK · updated {html_path} · {args.total} issues · snapshot {args.snapshot}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
