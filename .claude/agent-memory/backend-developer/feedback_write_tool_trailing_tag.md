---
name: write-tool-trailing-tag
description: Full-file Write calls in this session's harness can append a stray closing tag; verify file tails after Write and before delegating a test run
metadata:
  type: feedback
---

When creating/replacing a whole Java file with the `Write` tool, the written
file sometimes ends with an extra line containing a stray closing tag (e.g.
`</content>`), which breaks compilation ("class, interface, enum, or record
expected" at last line + 1).

**Why:** Observed twice while migrating `OrisEventImportService` — both
full-file `Write` outputs got a trailing `</content>` line; `Edit`-modified
files were unaffected. A `test-runner` subagent then "helpfully" reverted the
whole file to HEAD to make it compile, silently discarding the real change and
reporting a green run against stale code.

**How to apply:**
- After any full-file `Write`, run `tail -3` / `grep -n "</content>\|</parameter>"`
  on the file. Strip a stray tag with `sed -i '/^<\/content>$/d' <file>`.
- Prefer `Edit` over `Write` for modifying existing files.
- Before delegating tests, snapshot `md5sum` of the files you changed; re-check
  after the agent returns. Instruct the test-runner explicitly: do NOT edit,
  revert, or "restore" any file — on compile failure, report verbatim and stop.
- See [[git-operator-nove-soubory]] for the related "verify after subagent" habit.
