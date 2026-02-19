---
name: Review Hateoas Links for best practises 
description: Reviews HATEOAS links for best practises (link to GET api only, affordance to POST/PUT/PATCH/DELETE api,..) 
category: Klabis
tags: [klabis, review]
allowed-tools: Read(./*), Glob(./*), Grep(./*)
---

Review HATEOAS rules compliance in members module controllers and processors:                           
# Rules:                                                                                    
- link is added only for API endpoints with GET method                                         
- affordance is added only for API endpoints with POST/PUT/DELETE/PATCH method                          
# Additional instructions
For each controller and RepresentationModelProcessor in the members module:
1. Find all links and affordances being added
2. Check what HTTP method the target endpoint uses
3. Verify compliance with the rules

Organize the report BY TARGET ENDPOINT (the endpoint being linked/afforded to).

For each target endpoint, show:
- Endpoint path and HTTP method
- Source (which controller/processor adds it)
- Type (link or affordance)
- Status (✅ OK or ⚠️  PROBLEM)

Include a summary table at the end with totals.

