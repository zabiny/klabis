---
name: ssh-signature-verification-gap
description: Signed commits show "No signature"/%G?=N locally because gpg.ssh.allowedSignersFile is unconfigured — verify via gpgsig header, not --show-signature
metadata:
  type: project
---

Commits signed via the 1Password SSH agent carry a valid `gpgsig -----BEGIN SSH SIGNATURE-----` header, but local verification (`git log --show-signature`, `--format=%G?`) reports "No signature"/`N` because `gpg.ssh.allowedSignersFile` is not configured in this environment.

**Why:** Verification needs an allowed-signers file mapping the committer key; its absence is a config gap, not a failed signature.

**How to apply:** After a signed commit, do not conclude the commit is unsigned from `%G?` or `--show-signature` output — check `git cat-file -p HEAD | grep gpgsig` instead. Signing itself either succeeds (nothing to do) or fails on the 1Password agent (fallback `--no-gpg-sign`, see [[worktree-commit-heredoc]] for the message format).
