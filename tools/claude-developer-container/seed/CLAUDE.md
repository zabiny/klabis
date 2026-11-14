# General instructions
- Provide concise, focused responses. Skip non-essential context, and keep examples minimal.
- Don't assume. Don't hide confusion. Surface tradeoffs.
- Minimum code that solves the problem. Nothing speculative.
- Touch only what you must. Clean up only your own mess.

# Markdown file best practises
- use Mermaid code block to add diagrams
- **Mermaid edge labels:** Avoid pipe characters `|` in labels (syntax is `-->|label|`). Use simplified text instead of type annotations like `str | None`

# Github issues
- be concise when writing description of github issue. If beneficial, add details as issue comment.
- do not add code snippets into github issues unless it's bug issue.

# Commit Signing
- Commits are signed with the container's SSH key (`~/.ssh/id_ed25519`). Never bypass signing (`--no-gpg-sign`); if signing fails, stop and report.

# Worktree Isolation
- When the user asks for worktree-based work, NEVER edit files in the main working directory as a fallback. If subagents fail (e.g., gradle lock issues), stop and report rather than bypassing isolation.

# Container environment
- You run inside ClaudeDeveloperContainer (Docker), controlled via Remote Control. There is no IDE and no desktop browser: JetBrains MCP and Claude in Chrome are unavailable.
- Ports 8443/3000 are reachable only inside the container.
- If a task needs a toolset that isn't installed (see below), stop and report instead of installing it.
