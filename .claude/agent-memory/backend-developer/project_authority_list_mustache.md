---
name: project-authority-list-mustache
description: KlabisSpringCodegen renders full @HasAuthority/@ReadAuthority text into x-klabis-authority-annotation / x-klabis-read-authority-annotation; templates print verbatim (asLists removed)
metadata:
  type: project
---

`x-klabis-authority` / `x-klabis-read-authority` accept a string or a list in the spec. `KlabisSpringCodegen.renderAuthorityAnnotations` (fromOperation + postProcessModelProperty) joins them into the complete annotation text under `x-klabis-authority-annotation` / `x-klabis-read-authority-annotation`; `api.mustache` and `pojo.mustache` print it with `{{{.}}}` — same pattern as `x-klabis-halforms-annotation`.

**Why:** mustache cannot tell the last element of a bare string, so the old `-first/-last` + `asLists` trick was replaced by rendering in Java (verified: generated sources identical modulo blank lines).

**How to apply:** any new x-klabis-* extension that renders a Java array/annotation should render its text in the codegen, not in the template. Validator target-id rule (`operationTargetErrors` in validate.mjs) is one check per operation, not per key. `@OwnerId` is gone; use `@TargetId(TargetType.MEMBER)`.
