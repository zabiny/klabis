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

## GitHub Issues Workflow
- **Keep issues business-focused** - Describe WHAT and WHY, not HOW (implementation details belong in code/docs)
- **Split large issues** - Create child issues for subtasks, link to parent issue with comments
- **Use label to mark backend implementation done** - Add label `BackendCompleted` to issue when backend development is completed (do not close it yet)
- **Analysis documents** - Store temporary analysis in `/mnt/ramdisk/klabis/` before creating issues

**Example linking pattern:**
```bash
# Parent issue comment
- Child issue #264 - feat(oris): ORIS synchronizace
- Child issue #265 - feat(cus): CUS synchronizace
```

## GitHub CLI Usage

**Bash tool sandbox workaround:**
- `gh` commands require `dangerouslyDisableSandbox: true` due to bwrap permission errors
- Error: `bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted`

**Issue creation with heredoc:**
```bash
gh issue create --title "Title" --body "$(cat <<'EOF'
Multi-line body here...
EOF
)"
```

**Common commands:**
- Create issue: `gh issue create --title "..." --body "..."`
- Add comment: `gh issue comment <number> --body "..."`
- Add label: `gh issue edit <number> --add-label "LabelName"`

