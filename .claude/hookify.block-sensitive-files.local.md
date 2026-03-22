---
name: block-sensitive-files
enabled: true
event: file
tool_matcher: Edit|Write
action: block
conditions:
  - field: file_path
    operator: regex_match
    pattern: \.(env|secret)|http-client\.env
---

Sensitive file — do not edit credentials or env files.
