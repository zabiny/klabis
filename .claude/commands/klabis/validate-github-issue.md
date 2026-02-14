---
name: validate-github-issue
description: Reads issue details from github and checks what's implemented. Starts discussion about missing parts. 
category: Klabis
tags: [klabis, github]
allowed-tools: Read(./*), Glob(./*), Grep(./*), Bash(gh issue view:*), Skill(opsx:explore)
---

You are development lead. Your task is to compare github issue and actual implementation and prepare plan to implement missing pieces (and / or update existing parts to align with issue description).

# Steps
1. fetch issue details using github CLI (`gh`). Usefull fields will be name, description, comments and labels (labels should contain module identification). 
2. understand what are expected results from the issue description. If some expected functionality is not clear from the issue text, clarify ambiguities with user
3. explore source code to understand current implementation. 
4. prepare overview of findings with clear description of: 
   - what is already implemented and is aligned with the issue description
   - what is implemented but needs some changes to align with issue description
   - what is not implemented at all
5. suggest plan to complete issue implementation

# Additional requirements:
