---
name: Klabis functions summary
description: Create document with actual application functions summary 
category: Klabis
tags: [klabis, documentation]
allowed-tools: Read(./*), Write(./klabis-backend/docs), Skill(frontend-design:*)
---

Your task is to create simple webpage listing application functionalities in Czech language. Pages should help to get fast overview of application functions. 

Create webpage in `./klabis-backend/docs/summary` folder

Steps: 
1. familiarize yourself with completed openspec specifications = files in `./openspec/specs/**` folders. 
2. read `./klabis-backend/docs/summary/_template_README.md` to understand how created web pages should look like
3. delete existing pages (except template files and index.html)
4. create page for every specification showing overview of functions from such specification file. 
5. if needed update index.html with summaries and links to new pages  


# Additional requirements
- there should be subpage for every specification
- use specificaiton name as webpage name
- use frontend design skill to create page with distinct, modern and clean pleasing style
- all pages should share same design style
- it should be possible to navigate through pages using menu (ideally shared between all pages)

