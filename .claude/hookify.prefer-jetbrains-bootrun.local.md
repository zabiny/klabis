---
name: prefer-jetbrains-bootrun
enabled: true
event: bash
pattern: gradlew\s+bootRun
action: warn
---

Use JetBrains run configuration "Klabis Backend" instead of `./gradlew bootRun`.

The run configuration has correct environment variables and avoids sandbox/bwrap issues.
Use `mcp__jetbrains__execute_run_configuration` with `configurationName: "Klabis Backend"`.