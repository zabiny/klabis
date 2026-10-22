"""Shared naming: label names, gate branches and the issue reference in PR bodies.

The install copies this module unchanged into the target repository's `.github/sdlc/`,
so the server-side automation uses the same names as the CLI. Keep it standard-library
only and free of other sdlc imports.
"""

import re

GATES = ("specs", "design", "code")
PLACES = GATES + ("archive",)  # the archive PR has a branch and a reference but is no gate
TRACKS = ("small", "feature")
APPROVAL_GATES = GATES  # each gate PR, and the code PR, has an approval record

# Labels (github-backend: the concept-to-GitHub mapping). Opt-in is no label: it is
# an agent account among the issue's assignees.
WONT_DO = "agent:wontdo"
NEEDS_INFO = "agent:needs-info"
BLOCKED = "agent:blocked"
ESCAPED_BUG = "bug:escaped"
ALWAYS_HUMAN = "risk:human-required"
APPROVAL_PREFIX = "approved:"
TRACK_PREFIX = "track:"
GATE_PREFIX = "gate:"
TYPE_PREFIX = "type:"
WORKFLOW_IDENTITY = "github-actions[bot]"


def approval_mark(gate):
    return f"{APPROVAL_PREFIX}{gate}"


def track_mark(track):
    return f"{TRACK_PREFIX}{track}"


def gate_mark(gate):
    return f"{GATE_PREFIX}{gate}"

_BRANCH = re.compile(r"^sdlc/(\d+)-(specs|design|code|archive)$")
_REFERENCE = re.compile(r"<!-- sdlc:pr issue=(\d+) gate=(specs|design|code|archive) -->")


def gate_branch(issue, gate):
    """`sdlc/<issue>-<gate>`, or `sdlc/<issue>-archive` for the archive PR."""
    if gate not in PLACES:
        raise ValueError(f"unknown gate {gate}")
    return f"sdlc/{issue}-{gate}"


def parse_branch(name):
    """(issue, gate) for a gate or archive branch name, else None."""
    match = _BRANCH.match(name or "")
    if not match:
        return None
    return int(match.group(1)), match.group(2)


def issue_reference(issue, gate):
    """The PR body footer. Gate PRs and the code PR reference the issue without closing
    it; the archive PR closes it when merged."""
    keyword = "Closes" if gate == "archive" else "Refs"
    return f"{keyword} #{issue}\n\n<!-- sdlc:pr issue={issue} gate={gate} -->"


def parse_reference(body):
    """(issue, gate) from a PR body written with issue_reference, else None."""
    match = _REFERENCE.search(body or "")
    return (int(match.group(1)), match.group(2)) if match else None


def glob_to_regex(pattern):
    """Compile an always-human glob: `**/` any directories, `**` anything, `*` and `?` within
    one path segment."""
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif pattern.startswith("**", i):
            out.append(".*")
            i += 2
        elif pattern[i] == "*":
            out.append("[^/]*")
            i += 1
        elif pattern[i] == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(pattern[i]))
            i += 1
    return re.compile("^" + "".join(out) + "$")


def always_human_matches(globs_text, files):
    """The files matching any glob of `.github/sdlc-always-human.txt` (one glob per line,
    `#` comments and blank lines ignored), sorted."""
    patterns = [glob_to_regex(line.strip()) for line in (globs_text or "").splitlines()
                if line.strip() and not line.strip().startswith("#")]
    return sorted(f for f in files if any(p.match(f) for p in patterns))
