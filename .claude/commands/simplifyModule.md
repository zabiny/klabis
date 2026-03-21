---
name: simplify-module
description: simplifies code in specified module
context: fork
disable-model-invocation: true
---

       Deeply analyze the $0 module in the backend. Map out:
       1. All files in backend/src/main/java/$0/ and backend/src/test/java/$0/
       2. The architecture layers (domain, application, infrastructure/adapters, API/web)
       3. Key classes, their responsibilities, and dependencies between them
       4. Any code smells: duplication, over-engineering, unnecessary abstractions, dead code, inconsistencies
       5. Test coverage patterns

       Focus on identifying simplification opportunities:
       - Unused code or imports
       - Over-abstracted patterns
       - Duplicated logic
       - Unnecessary complexity
       - Code that could be consolidated

       Be thorough - read all source files in the module.

