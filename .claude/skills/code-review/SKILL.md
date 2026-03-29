---
name: code-review
description: Instructions for code review on klabis project. Use everytime when code review is requested. 
user-invocable: false
---

# Code review areas

- project best practises
- testing best practises
- test coverage of new features, review of tests for changed or removed parts for changes / unnecessary tests
- remove dead or unused code
- KISS

# Additional criteria for HATEOAS API

- if API response contains ID referencing another domain object, it must contain also _link record for such domain object
  - For example Event detail contains `coordinatorId` - refenrece to Member object -> it must also contain `_links.coordinator.href` in HAL+JSON response 