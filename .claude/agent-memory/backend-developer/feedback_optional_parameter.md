---
name: optional-parameter
description: Never use Optional as a method parameter — use boolean flag + nullable, or a dedicated type
metadata:
  type: feedback
---

Never use `Optional<T>` as a method parameter.

**Why:** The user explicitly rejected `Optional<MemberId>` as a port parameter. Optional is designed for return types, not parameters — it adds unnecessary wrapping and is considered bad practice in idiomatic Java.

**How to apply:** When you need to express a tri-state (e.g., "not requested / requested with value / requested without value"), use two explicit parameters: a `boolean requestedFlag` + `@Nullable T value`. Or introduce a dedicated record/enum if more states are needed. Never wrap a parameter in Optional.
