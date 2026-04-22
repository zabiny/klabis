---
name: block-direct-git
enabled: true
event: bash
pattern: ^\s*git\s+(commit|push|pull|merge|rebase|checkout|branch|tag|stash|reset|cherry-pick|fetch|add|rm|mv|restore|switch|clone|init|remote)
action: warn
---

⚠️ **Přímý git příkaz detekován**

Git operace musí být prováděny výhradně přes **git-operator subagent**, ne přímými `git` příkazy v Bash tool.

**Co dělat místo toho:**

Spusť `git-operator` agenta s popisem požadované git operace:
```
Agent({
  subagent_type: "git-operator",
  description: "...",
  prompt: "Proveď git commit s tímto popisem: ..."
})
```

**Proč:**
- git-operator agent správně řeší GPG podepisování, pre-commit hooky a edge cases
- Konzistentní git workflow napříč projektem
- Bezpečnější — agent ví jak reagovat na chyby (např. signing failures)

Pokud jde o čtecí git příkaz (git log, git status, git diff, git show, git blame), ten je povolen — pravidlo blokuje pouze modifikující operace.
