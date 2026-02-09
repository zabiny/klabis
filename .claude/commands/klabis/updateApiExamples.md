---
name: update API examples
description: Updates API examples in HTTP files from current OpenAPI specs
category: Klabis
tags: [klabis, docs]
allowed-tools: Read(./*), Edit(./*), Glob(./*), Grep(./*), Write(./*), Bash(git:*)
---

Your task is to update API examples in IntelliJ HTTP files from current OpenAPI specs generated from Spring MVC. 

# Steps: 

1. start backend API server 
2. download OpenAPI specs from http://localhost:8080/v3/api-docs 
3. stop backend API server
4. check ./klabis-backend/docs/API.md file and update any incorrect information there  
4. check all .http files in ./klabis-backend/docs/examples and update any incorrect information there or add missing calls 
5. commit updated docs files to git

# Additional requirements: 
- HTTP requests are split by domain - every domain have own .http file named as [domain].http. (for example members.http)
- HTTP files should contain example for every API call