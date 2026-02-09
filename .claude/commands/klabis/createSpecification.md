---
name: create specification
description: Guides user through openspec specification proposal process and finalizes it after proposal is approved by user
category: Klabis
tags: [klabis, workflow]
allowed-tools: Read(./*), Edit(./openspec/*), Glob(./*), Grep(./*), Write(./openspec/*), Bash(git:*), Skill(openspec:*), Skill(openspec-proposal-summary:*)
---

You are helping to business analyst/architect to walk them through creation of openspec proposal from start to the proposal approve and it's commit to master branch

Steps required: 
1. check that you work on master branch. If not, inform user that specification can be proposed only on master branch and ask him if he wants to checkout specs branch and continue. If he rejects, interrupt this process. 

2. fetch latest changes and update master branch to have newest version of specs and code

3. be helpful assistant while user works on new proposal and make sure standard openspec proposal steps are followed. Inform user that once he is satisfied with the proposal, he shall write "proposal completed". 

4. After user completes proposal, use openspec tools to verify format correctness of approval.

5. Then perform crosscheck of created proposal documents for possible inconsitencies, unclear areas and design gaps. If something shows up, inform user and continue with (2) 

6. Ask user if proposal is ready for implementation, ready for stakeholder approval or if he wants to further refine proposal. 
    - if user tells that proposal is ready for implementation, continue at (6)
    - if user tells that proposal is ready for stakeholder approval, then create stakeholder review document for this proposal and show user full path to created document. Do not continue to next steps until user confirms that stakeholders approved proposal. If user will want to further refine proposal, continue at (2) 
    - if user will want to further refine the proposal, continue at (2)  

7. create git commit with proposal

