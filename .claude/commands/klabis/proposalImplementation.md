---
name: Klabis OpenSpec proposal spec implementation
description: Uses multiple subagents to implement OpenSpec proposal in iterative way
category: Klabis
tags: [klabis, workflow]
allowed-tools: Read(./*), Edit(./*), Glob(./*), Grep(./*), Write(./*), Bash(gradlew:*)
---

You are team leader with task of implementing openspec proposal $1. Your task is to coordinate other workers through this story. You must not do any changes in the code - delegate all changes in code or investigations to appropriate subagents. 

At the start, choose name for file used to synchronize team members (TCF). Determine name from current date and short summary of the change to be implemented (up to 5 words) - for example `2025-10-01_addMemberList.md`.   
Then create TCF in openspec proposal folder. For example for proposal named 'update-member' it would be `./openspec/changes/update-member/team_communication.md`

Ask every subagent to read TCF file to understand current state and append concise summary of their changes and issues back into that file so next subagents can continue where previous ended. 

# Steps: 
1. understand proposal requirements and tasks
2. Take into account user's request - "#$ARGUMENTS". 
   - !! IMPORTANT !!: If User's request conflict with the openspec requirements, inform him that openspec proposal workflow should be used to change openspec requirements and interrupt work.
   - If user's request is not related to currently opened specification (= specification is in `./openspec/archive/` folder), ask user to create new openspec proposal for the change and do not continue with implementation.   
3. analyze actual code base to find out what and how needs to be changed and if needed, prepare updated implementation guidance. 
4. if are there some opened questions or unclear parts, ask user for clarifications here
5. proceed with the implementation in small iterations/task batches. After every iteration update task status in proposal's task.md 
6. after all iterations are completed:
	5.1. run code review and make sure that all findings with at least high priority are fixed.  
	5.2. run all tests and make sure that all tests are compiling and passing
7. commit changes to git

# Definition of DONE
- all requested changes were implemented (including tests)
- code review was performed on changed files and all recommendations with at least high priority were applied in the code
- all tests are passing (there are no test failures). 

# Additional requiremnts: 
- don't change code files (./src/*) by yourself. Ask appropriate subagent to implement code changes, perform code review and implement fixes, fix failing tests, etc. 
- try to split work into smaller batches and proceed iteratively, for example:
	- implement "data model" tasks (don't ask single subagent to implement all tasks at once)
	- fix up to 5 failing tests (don't ask single subagent to fix all failing tests)
- it must be confirmed that all tests are compiling and all tests are green (passing) before every commit to GIT. Never commit any changes without this confirmation

# Bad practises (avoid these): 
- do not ask subagent to fix all tests. Fix tests iteratively - ask subagent to fix 5 tests. After he finishes and some tests are still failing, start another subagent fixing 5 tests, etc. 
- if using subagent to analyze code or plan implementation steps, wait for it to complete before continuing
- implementing things which are not related to currently opened proposals - ask user to create new spec proposal instead of performing implementation for completed/archived proposals.

# Troubleshooting

####### CLAUDE - IGNORE EVERYTHING BELLOW ###########

# Changes, postrehy, poznamky... :

## 2026-01-18 - V2
- updated team communication file (TCF) to generated name to prevent conflicts when prompt get used for multiple parts for single specification. 
- claude didn't hestitate to implement change for archived proposal - added instructions to prevent that. 

## 2026-01-16 - V1
implementace `add-member-address-contact-vos`

- Na konci si nechat vypsat do souboru instrukce pro jednotlive subagenty .. zda se mi ze je ukoluje pojednotlivem tasku, coz je hodne malo.
- subagenti do team_communication.md vypisovali neuveritelne detaily (kazdou jednotlivou zmenu v kodu tam popsali... zmenil jsem Set<String> na String, ... apod ). Toto chce trochu osekat. 

