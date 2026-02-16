#!/usr/bin/env python3
"""Parse JUnit XML (backend) or Vitest JSON (frontend) test results and generate formatted report."""

import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_backend_xml(test_results_dir: str, full_output: bool) -> str:
    results_path = Path(test_results_dir)
    if not results_path.exists():
        return format_report("BACKEND", 0, 0, 0, 0, [])

    xml_files = list(results_path.glob("TEST-*.xml"))
    if not xml_files:
        return format_report("BACKEND", 0, 0, 0, 0, [])

    total = 0
    failed = 0
    errors = 0
    skipped = 0
    failures = []

    for xml_file in xml_files:
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
        except ET.ParseError:
            continue

        suite_tests = int(root.attrib.get("tests", 0))
        suite_failures = int(root.attrib.get("failures", 0))
        suite_errors = int(root.attrib.get("errors", 0))
        suite_skipped = int(root.attrib.get("skipped", 0))

        total += suite_tests
        failed += suite_failures
        errors += suite_errors
        skipped += suite_skipped

        for testcase in root.findall("testcase"):
            failure_el = testcase.find("failure")
            error_el = testcase.find("error")
            if failure_el is not None:
                el = failure_el
                error_type = "FAILURE"
            elif error_el is not None:
                el = error_el
                error_type = "ERROR"
            else:
                continue

            classname = testcase.attrib.get("classname", "")
            name = testcase.attrib.get("name", "")
            test_id = f"{classname}.{name}" if classname else name
            message = el.attrib.get("message", "")
            raw_trace = el.text or ""

            if full_output:
                trace_lines = raw_trace.strip()
            else:
                trace_lines = filter_stacktrace(raw_trace)

            failures.append({
                "type": error_type,
                "test": test_id,
                "message": message,
                "stacktrace": trace_lines,
            })

    total_failed = failed + errors
    passed = total - total_failed - skipped
    return format_report("BACKEND", total, passed, total_failed, skipped, failures)


def filter_stacktrace(raw: str) -> str:
    """Keep only application code lines from stacktrace."""
    lines = raw.strip().split("\n")
    kept = []
    for line in lines:
        stripped = line.strip()
        # Keep assertion/exception message lines (not starting with "at ")
        if not stripped.startswith("at "):
            kept.append(stripped)
            continue
        # Keep application code frames
        if "com.klabis." in stripped:
            kept.append(stripped)
    return "\n".join(kept)


def parse_frontend_json(json_path: str, full_output: bool) -> str:
    if not os.path.exists(json_path):
        return format_report("FRONTEND", 0, 0, 0, 0, [])

    with open(json_path, "r") as f:
        data = json.load(f)

    total = data.get("numTotalTests", 0)
    passed = data.get("numPassedTests", 0)
    failed_count = data.get("numFailedTests", 0)
    skipped = total - passed - failed_count

    failures = []
    for suite in data.get("testResults", []):
        for result in suite.get("assertionResults", []):
            if result.get("status") != "failed":
                continue
            test_name = " > ".join(result.get("ancestorTitles", []) + [result.get("title", "")])
            messages = result.get("failureMessages", [])
            trace = "\n".join(messages)
            if not full_output:
                trace = filter_frontend_stacktrace(trace)
            failures.append({
                "type": "FAILURE",
                "test": test_name,
                "message": "",
                "stacktrace": trace,
            })

    return format_report("FRONTEND", total, passed, failed_count, skipped, failures)


def filter_frontend_stacktrace(raw: str) -> str:
    """Keep relevant lines from frontend stacktrace."""
    lines = raw.strip().split("\n")
    kept = []
    for line in lines:
        stripped = line.strip()
        # Keep assertion messages and src/ references
        if "src/" in stripped or "expect" in stripped.lower() or "error" in stripped.lower() or not stripped.startswith("at "):
            kept.append(stripped)
    return "\n".join(kept)


def format_report(test_type: str, total: int, passed: int, failed: int, skipped: int, failures: list) -> str:
    lines = []
    lines.append("=" * 70)
    lines.append(f"TEST EXECUTION REPORT - {test_type}")
    lines.append("=" * 70)
    lines.append("")

    status = "✅ PASSED" if failed == 0 else "❌ FAILED"
    lines.append(f"Status: {status}")
    lines.append(f"Total: {total} | Passed: {passed} | Failed: {failed} | Skipped: {skipped}")
    lines.append("")

    if failures:
        lines.append("FAILED TESTS")
        lines.append("-" * 70)

        for idx, f in enumerate(failures, 1):
            lines.append(f"")
            lines.append(f"{idx}. {f['type']}: {f['test']}")
            trace = f["stacktrace"].strip()
            if trace:
                for trace_line in trace.split("\n"):
                    lines.append(f"   {trace_line}")
            lines.append("")

    lines.append("=" * 70)
    return "\n".join(lines)


def main():
    if len(sys.argv) < 2:
        print("Usage: parse-test-output.py <backend|frontend> [--full]")
        print("  backend: parses JUnit XML from backend/build/test-results/test/")
        print("  frontend: parses Vitest JSON from /tmp/claude/vitest-results.json")
        print("  --full: show complete stacktraces (not just app code)")
        sys.exit(1)

    test_type = sys.argv[1]
    full_output = "--full" in sys.argv

    # Find project root (4 levels up from this script)
    script_dir = Path(__file__).resolve().parent
    project_root = script_dir.parent.parent.parent.parent

    if test_type == "backend":
        results_dir = project_root / "backend" / "build" / "test-results" / "test"
        report = parse_backend_xml(str(results_dir), full_output)
    elif test_type == "frontend":
        json_path = "/tmp/claude/vitest-results.json"
        report = parse_frontend_json(json_path, full_output)
    else:
        print(f"Unknown test type: {test_type}")
        sys.exit(1)

    print(report)


if __name__ == "__main__":
    main()
