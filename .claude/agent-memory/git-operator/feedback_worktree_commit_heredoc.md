---
name: worktree-commit-heredoc
description: In git worktree sessions, heredoc commit messages are rejected by the worktree sandbox — use multiple -m flags instead
metadata:
  type: feedback
---

When committing from a worktree-isolated session (`.claude/worktrees/...`), the sandbox rejects compound git commands including the `git commit -m "$(cat <<'EOF' ... EOF)"` heredoc form, even with plain `git commit`.

**Why:** The sandbox cannot verify that a complex command string stays inside the worktree, so it refuses to run it.

**How to apply:** Build multi-paragraph messages with separate `-m` flags (subject, body, trailer each as its own `-m`) and run git from the worktree directory without `cd` chains. Escape `$` in message text (e.g. `\$ref`) to prevent shell expansion. Signed commits work normally — the 1Password failure fallback (`--no-gpg-sign`, see [[feedback_git_quirks]]) is still only a fallback.
