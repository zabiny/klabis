---
name: hateoas-api-overview
description: This skill should be used when the user asks to "generate HATEOAS docs", "document API authorization", "list endpoints with links"
context: fork
model: Sonnet
version: 1.0.0
---

Every backend controller returns HAL+FORMS response including `_links`. Links are added using @backend/src/main/java/com/klabis/common/ui/HalFormsSupport.java  class.

Create list of all API endpoints with link relations returned in such endpoint. For every link, check if target method requires some authorization (Spring Security annotations on method handler, `HasAuthority` anotation) and if yes, describe required authorization for that link/affordance.

create file klabis-hateoas-api-overview.md (in project root) with this overview (delete old file if it exists)
