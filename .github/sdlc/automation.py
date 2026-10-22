#!/usr/bin/env python3
"""Server-side SDLC automations, run by the `sdlc-*.yml` workflows on GitHub events.

Installed by the agentic-sdlc install skill. Every workflow checks out the default
branch, so this script and the roles file always come from it, never from a pull
request. Standard library only; `roles.py`, `naming.py` and `yamlsubset.py` next to
it are copies of the SDLC CLI's modules.

Subcommands:
  guard          issues labeled/unlabeled: keep opt-in, won't-do and approval labels
                 to the people and automations allowed to change them
  mention        issues opened, issue_comment created: `@sdlc` by a role holder opts
                 the issue in
  review-mirror  pull_request_review submitted/dismissed, pull_request_target
                 synchronize/closed: keep approved:specs / approved:design /
                 approved:code in step with the gate and code PRs' reviews, and clear
                 the gate label when the archive PR merges
  always-human   pull_request_target opened/synchronize/reopened: add or remove
                 risk:human-required from the PR's changed files
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from urllib.parse import quote

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import naming  # noqa: E402
import roles as roles_module  # noqa: E402

PLUGIN_VERSION = "0.2.4"
ALWAYS_HUMAN_PATH = ".github/sdlc-always-human.txt"
GUARDED_STATUS = (naming.WONT_DO,)
APPROVALS = tuple(naming.approval_mark(g) for g in naming.APPROVAL_GATES)
GATE_ROLE = {"specs": "stakeholder", "design": "developer", "code": "developer"}
ROLE_TITLE = {"stakeholder": "Stakeholder", "developer": "Developer"}
MENTION = re.compile(r"(?<![\w@/.-])@sdlc(?![\w-])", re.IGNORECASE)
MARKER = "<!-- sdlc:automation {} -->"


class RolesUnreadable(Exception):
    pass


# --- GitHub ------------------------------------------------------------------------------

class GitHub:
    """The few REST calls the automations make, with the workflow's GITHUB_TOKEN."""

    def __init__(self, repo, token, api=None):
        self.repo = repo
        self.token = token
        self.api = (api or "https://api.github.com").rstrip("/")

    def _call(self, method, path, body=None):
        results, url = [], f"{self.api}/repos/{self.repo}/{path}"
        while url:
            data = json.dumps(body).encode() if body is not None else None
            request = urllib.request.Request(url, data=data, method=method, headers={
                "Authorization": f"Bearer {self.token}",
                "Accept": "application/vnd.github+json",
                "X-GitHub-Api-Version": "2022-11-28",
                "User-Agent": "sdlc-automation",
            })
            try:
                with urllib.request.urlopen(request) as response:
                    text = response.read().decode()
                    link = response.headers.get("Link") or ""
            except urllib.error.HTTPError as exc:
                if method == "DELETE" and exc.code == 404:
                    return None  # label already gone
                raise SystemExit(f"GitHub {method} {path} failed: {exc.code} {exc.read().decode()[:300]}")
            value = json.loads(text) if text else None
            if not isinstance(value, list):
                return value
            results.extend(value)
            match = re.search(r'<([^>]+)>;\s*rel="next"', link)
            url = match.group(1) if match else None
        return results

    def issue_labels(self, number):
        return [label["name"] for label in self._call("GET", f"issues/{number}/labels?per_page=100")]

    def add_label(self, number, name):
        self._call("POST", f"issues/{number}/labels", {"labels": [name]})

    def remove_label(self, number, name):
        self._call("DELETE", f"issues/{number}/labels/{quote(name, safe='')}")

    def issue_assignees(self, number):
        return [a["login"] for a in self._call("GET", f"issues/{number}").get("assignees") or []]

    def assign(self, number, login):
        self._call("POST", f"issues/{number}/assignees", {"assignees": [login]})

    def unassign(self, number, login):
        self._call("DELETE", f"issues/{number}/assignees", {"assignees": [login]})

    def comments(self, number):
        return [c["body"] or "" for c in self._call("GET", f"issues/{number}/comments?per_page=100")]

    def comment(self, number, body):
        self._call("POST", f"issues/{number}/comments", {"body": body})

    def pull(self, number):
        return self._call("GET", f"pulls/{number}")

    def reviews(self, number):
        return self._call("GET", f"pulls/{number}/reviews?per_page=100")

    def pr_files(self, number):
        files = []
        for entry in self._call("GET", f"pulls/{number}/files?per_page=100"):
            files.append(entry["filename"])
            if entry.get("previous_filename"):
                files.append(entry["previous_filename"])
        return files


# --- shared helpers ------------------------------------------------------------------------

def load_roles(root):
    path = os.path.join(root, roles_module.ROLES_PATH)
    try:
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
    except FileNotFoundError:
        raise RolesUnreadable(f"{roles_module.ROLES_PATH} does not exist on the default branch") from None
    except (OSError, UnicodeDecodeError) as exc:
        raise RolesUnreadable(f"{roles_module.ROLES_PATH} could not be read: {exc}") from None
    try:
        return roles_module.parse_roles(text)
    except roles_module.RolesError as exc:
        raise RolesUnreadable(f"{roles_module.ROLES_PATH} is not valid: {exc}") from None


def fail_closed(gh, number, automation, problem):
    """No label change; tell the issue or PR, and fail the run."""
    gh.comment(number, f"SDLC {automation}: the roles could not be read, so no label was changed."
                       f" {problem}. Fix `{roles_module.ROLES_PATH}` on the default branch."
                       f"\n\n{MARKER.format('roles-unreadable')}")
    print(f"error: {problem}", file=sys.stderr)
    return 1


def comment_once(gh, number, key, body):
    marker = MARKER.format(key)
    if any(marker in c for c in gh.comments(number)):
        return False
    gh.comment(number, f"{body}\n\n{marker}")
    return True


def is_workflow_identity(user):
    return (user or {}).get("login") == naming.WORKFLOW_IDENTITY


# --- guard ---------------------------------------------------------------------------------

def guard_assignment(event, gh, root):
    """Opt-in is the agent account as assignee: undo its assignment or unassignment by
    anyone without a Stakeholder or Developer role, the agent account included."""
    if is_workflow_identity(event.get("sender")):
        return 0  # the mention handler's own assignment
    number, sender = event["issue"]["number"], event["sender"]["login"]
    assignee = (event.get("assignee") or {}).get("login")
    try:
        roles = load_roles(root)
    except RolesUnreadable as exc:
        return fail_closed(gh, number, "guard", str(exc))
    if not roles.is_agent(assignee) or roles.is_role_holder(sender):
        return 0  # other people's assignments are not the guard's business
    added = event["action"] == "assigned"
    if roles.is_agent(sender):
        rule = "agents never change opt-in"
    else:
        rule = "only a Stakeholder or Developer can opt an issue in or out"
    if added:
        gh.unassign(number, assignee)
    else:
        gh.assign(number, assignee)
    verb = "assigned" if added else "unassigned"
    undo = "unassigned it again" if added else "assigned it back"
    gh.comment(number, f"SDLC guard: @{sender} {verb} @{assignee}, so the guard {undo}: {rule}."
                       f"\n\n{MARKER.format('guard')}")
    return 0


def guard(event, gh, root):
    if event.get("action") in ("assigned", "unassigned"):
        return guard_assignment(event, gh, root)
    action, label = event.get("action"), (event.get("label") or {}).get("name")
    if action not in ("labeled", "unlabeled") or label not in GUARDED_STATUS + APPROVALS:
        return 0
    if is_workflow_identity(event.get("sender")):
        return 0  # the automations' own changes
    number, sender = event["issue"]["number"], event["sender"]["login"]
    try:
        roles = load_roles(root)
    except RolesUnreadable as exc:
        return fail_closed(gh, number, "guard", str(exc))

    holder = roles.is_role_holder(sender)
    added = action == "labeled"
    if label in GUARDED_STATUS:
        if holder:
            return 0
        if roles.is_agent(sender):
            rule = "agents never change won't-do"
        else:
            rule = "only a Stakeholder or Developer can mark an issue won't-do or clear it"
    elif added:
        gate = label[len(naming.APPROVAL_PREFIX):]
        role = ROLE_TITLE[GATE_ROLE[gate]]
        rule = (f"approvals are given by reviewing the gate PR: the {role} approves the"
                f" {gate} PR, and the review mirror records it")
    else:
        if holder or roles.is_agent(sender):
            return 0  # a withdrawal stands; the guard does not judge why
        rule = "only a Stakeholder, a Developer or the agent account can withdraw an approval"

    if added:
        gh.remove_label(number, label)
    else:
        gh.add_label(number, label)
    verb = "added" if added else "removed"
    undo = "removed it again" if added else "put it back"
    gh.comment(number, f"SDLC guard: @{sender} {verb} `{label}`, so the guard {undo}: {rule}."
                       f"\n\n{MARKER.format('guard')}")
    return 0


# --- mention -------------------------------------------------------------------------------

def mention(event, gh, root):
    issue = event.get("issue") or {}
    if issue.get("pull_request"):
        return 0  # @sdlc opts issues in, not pull requests
    if "comment" in event:
        if event.get("action") != "created":
            return 0
        body, author = event["comment"].get("body"), event["comment"].get("user")
    else:
        if event.get("action") != "opened":
            return 0
        body, author = issue.get("body"), issue.get("user")
    if not MENTION.search(body or "") or is_workflow_identity(author):
        return 0
    number, login = issue["number"], (author or {}).get("login")
    try:
        roles = load_roles(root)
    except RolesUnreadable as exc:
        return fail_closed(gh, number, "mention", str(exc))
    if roles.is_role_holder(login):
        if not roles.agents:
            return 0  # no agent account to assign
        assigned = [a["login"] for a in issue.get("assignees") or []]
        if not any(roles.is_agent(a) for a in assigned) and \
                not any(roles.is_agent(a) for a in gh.issue_assignees(number)):
            gh.assign(number, roles.agents[0])
        return 0
    comment_once(gh, number, "mention-refused",
                 f"SDLC: @{login} mentioned @sdlc, but only a Stakeholder or Developer can opt an"
                 f" issue in, so the agent account was not assigned.")
    return 0


# --- review mirror -------------------------------------------------------------------------

def place(pr):
    """(issue, gate) of an SDLC PR from its branch name and issue link. None when the PR
    has neither (not an SDLC PR), and False when it has one of them only or they disagree."""
    branch = naming.parse_branch((pr.get("head") or {}).get("ref"))
    reference = naming.parse_reference(pr.get("body"))
    if not branch and not reference:
        return None
    return branch if branch == reference else False


def counting_approval(reviews, head_sha, roles, role):
    """Reviewers whose latest decisive review approves the current head and who hold `role`."""
    latest = {}
    for review in sorted(reviews, key=lambda r: (r.get("submitted_at") or "", r.get("id") or 0)):
        state, login = review.get("state"), (review.get("user") or {}).get("login")
        if login and state in ("APPROVED", "CHANGES_REQUESTED", "DISMISSED"):
            latest[login] = review
    has_role = roles.is_stakeholder if role == "stakeholder" else roles.is_developer
    return sorted(login for login, review in latest.items()
                  if review["state"] == "APPROVED" and review.get("commit_id") == head_sha
                  and has_role(login) and not roles.is_agent(login))


def review_mirror(event, gh, root):
    pr = event.get("pull_request") or {}
    number = pr.get("number")
    reviewed = "review" in event
    placed = place(pr)
    if event.get("action") == "closed":
        return clear_gate(gh, placed, pr)
    if placed is None:
        return 0  # not an SDLC PR
    if placed is False:
        if reviewed:
            comment_once(gh, number, "unplaced",
                         "SDLC review mirror: this PR lacks either a gate branch name"
                         " (`sdlc/<issue>-<gate>`) or an SDLC issue link, or they disagree, so the linked issue or gate could"
                         " not be found and no approval record was changed.")
        return 0
    issue, gate = placed
    if gate not in naming.APPROVAL_GATES:
        return 0  # the archive PR has no approval record
    try:
        roles = load_roles(root)
    except RolesUnreadable as exc:
        return fail_closed(gh, number, "review mirror", str(exc))

    role = GATE_ROLE[gate]
    pr = gh.pull(number)  # the current head, not the one in a possibly late event
    approvers = counting_approval(gh.reviews(number), pr["head"]["sha"], roles, role)
    label = naming.approval_mark(gate)
    labels = gh.issue_labels(issue)
    if approvers and label not in labels:
        gh.add_label(issue, label)
    elif not approvers and label in labels:
        gh.remove_label(issue, label)

    review = event.get("review") or {}
    reviewer = (review.get("user") or {}).get("login")
    if (event.get("action") == "submitted" and (review.get("state") or "").upper() == "APPROVED"
            and reviewer not in approvers and review.get("commit_id") == pr["head"]["sha"]):
        gh.comment(number, f"SDLC review mirror: @{reviewer}'s approval does not count for the {gate}"
                           f" gate, which belongs to the {ROLE_TITLE[role]} (see"
                           f" `{roles_module.ROLES_PATH}`), so `{label}` was not added to #{issue}."
                           f"\n\n{MARKER.format('wrong-role')}")
    return 0


def clear_gate(gh, placed, pr):
    """A merged archive PR ends the flow: remove the issue's gate position. The approval
    records stay, and an issue closed any other way keeps its labels."""
    if not placed or placed[1] != "archive" or not pr.get("merged"):
        return 0
    issue = placed[0]
    for label in gh.issue_labels(issue):
        if label.startswith(naming.GATE_PREFIX):
            gh.remove_label(issue, label)
    return 0


# --- always-human --------------------------------------------------------------------------

def always_human(event, gh, root):
    pr = event.get("pull_request") or {}
    number = pr["number"]
    try:
        with open(os.path.join(root, ALWAYS_HUMAN_PATH), encoding="utf-8") as handle:
            globs = handle.read()
    except FileNotFoundError:
        print(f"warning: {ALWAYS_HUMAN_PATH} does not exist; no path counts as always-human",
              file=sys.stderr)
        globs = ""
    matches = naming.always_human_matches(globs, gh.pr_files(number))
    labels = gh.issue_labels(number)  # current labels, not the event's
    if matches and naming.ALWAYS_HUMAN not in labels:
        gh.add_label(number, naming.ALWAYS_HUMAN)
    elif not matches and naming.ALWAYS_HUMAN in labels:
        gh.remove_label(number, naming.ALWAYS_HUMAN)
    for path in matches:
        print(f"always-human: {path}")
    return 0


AUTOMATIONS = {
    "guard": (guard, "keep opt-in, won't-do and approval labels to those allowed to change them"),
    "mention": (mention, "opt an issue in when a Stakeholder or Developer mentions @sdlc"),
    "review-mirror": (review_mirror, "keep the approval record in step with gate PR reviews"),
    "always-human": (always_human, "flag PRs touching always-human paths with risk:human-required"),
}


def main(argv=None, gh=None, environ=None):
    environ = os.environ if environ is None else environ
    parser = argparse.ArgumentParser(
        prog="automation.py", description=f"SDLC server-side automations (agentic-sdlc {PLUGIN_VERSION}).")
    parser.add_argument("--root", default=environ.get("GITHUB_WORKSPACE", "."),
                        help="checkout of the default branch (default: $GITHUB_WORKSPACE)")
    parser.add_argument("--event", default=environ.get("GITHUB_EVENT_PATH"),
                        help="event payload JSON (default: $GITHUB_EVENT_PATH)")
    sub = parser.add_subparsers(dest="automation", required=True, metavar="automation")
    for name, (_, help_text) in AUTOMATIONS.items():
        sub.add_parser(name, help=help_text)
    args = parser.parse_args(argv)
    if not args.event:
        parser.error("no event payload: set GITHUB_EVENT_PATH or pass --event")
    with open(args.event, encoding="utf-8") as handle:
        event = json.load(handle)
    if gh is None:
        gh = GitHub(environ["GITHUB_REPOSITORY"], environ["GITHUB_TOKEN"], environ.get("GITHUB_API_URL"))
    return AUTOMATIONS[args.automation][0](event, gh, args.root)


if __name__ == "__main__":
    sys.exit(main())
