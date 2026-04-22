---
name: block-direct-tests
enabled: true
event: bash
pattern: (gradlew?\s+.*test|vitest|npm\s+(run\s+)?test)
action: warn
---

⚠️ **Přímé spuštění testů detekováno**

Testy musí být spouštěny výhradně přes **developer:test-runner** subagenta, ne přímými příkazy v Bash tool.

**Co dělat místo toho:**

Spusť `developer:test-runner` agenta:
```
Agent({
  subagent_type: "developer:test-runner",
  description: "Run tests",
  prompt: "Spusť testy pro modul X / frontend"
})
```

**Proč:**
- test-runner agent správně reportuje výsledky v konzistentním formátu
- Řeší chyby a opakování podle projektu-specific konvencí
- Viz feedback memory: no-parallel-gradle — souběžné Gradle volání způsobují deadlock na cache locks
