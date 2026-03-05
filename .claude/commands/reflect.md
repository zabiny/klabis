---
name: reflect
description: Reflects problems from current conversation and updates CLAUDE.md
category: Common
allowed-tools: Read(./*), Edit(./*), Glob(./*), Grep(./*), Write(./*)
---

1. Based on this conversation and mistakes you done, prepare summary of updates to be done in CLAUDE.md should be done
   to advice with correct actions to prevent repeating same mistakes in the future?
2. Update CLAUDE.md file(s) with these. If file contains incorrect information, remove or replace it with corrected
   version. If is there something missing, add new.

# Additional requirements

- write concise versions of instruction, do not over-explain things. 