---
name: conversation-feedback
description: Creates html file with evaluation of current conversation - what went well and what caused issues
disable-model-invocation: true
context: fork
model: sonnet
---

Your task is to evaluate current conversation and provide feedback about what has went well and what caused issues.

# Additional requirements
- Save feedback into HTML file '.do-not-commit/feedbacks/<dd-MM-yyyy-HHmmss>-<slug>.html' (create slug as 3-5 word description of conversation subject)
- file shall contain:
    - short summary of subject of the conversation
    - summary of steps/actions done through this conversation
    - what went well
    - what didn't went well (caused issues, required user intervention/inputs in middle of your work, etc)
    - metrics (number of created commits, how many lines of code were produced/deleted, how many interventions were needed by user in middle of your work, how many tokens were consumed, etc..)
    - what took most of the time
    - what was most valuable thing done through the conversation
