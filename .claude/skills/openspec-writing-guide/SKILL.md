---
name: openspec-writing-guide
description: Klabis-specific guide for writing OpenSpec specifications. Use this skill whenever writing, reviewing, or rewriting any spec.md file in openspec/specs/. This skill defines the canonical style — user-facing scenarios, UI-context organization, and what belongs in specs vs. non-functional-requirements. Use proactively any time a spec is being created or modified, even if the user doesn't explicitly ask for style guidance.
user-invocable: false
version: 0.1.0
---

# OpenSpec Writing Guide

Canonical rules for writing specifications in the Klabis project. The reference example of correct style is `openspec/specs/user-groups/spec.md`.

## Core Principle: User-Facing Scenarios

Specs describe **what the user experiences**, not what the system does internally.

**Wrong (API-centric):**
> WHEN authenticated user with MEMBERS:CREATE permission submits POST /api/members
> THEN HTTP 201 Created is returned with Location header

**Right (user-facing):**
> WHEN admin fills in member registration form and clicks "Registrovat člena"
> THEN the new member appears in the member list
> AND the system sends a welcome email to the provided address

Every scenario should be understandable by a non-technical stakeholder.

## Scenario Format

Use the same WHEN/THEN/AND structure as user-groups spec:

```markdown
#### Scenario: <descriptive name from user's perspective>

- **WHEN** <user action or system state>
- **THEN** <what the user sees or what happens>
- **AND** <additional outcomes>
```

Use **GIVEN** only when prior state needs to be established explicitly.

## Organization: UI Contexts, Not API Endpoints

Group requirements by **what the user is looking at**, not by HTTP method or endpoint.

Good sections for members spec:
- **Member Registration** — the registration form page
- **Member List** — the list/table page
- **Member Detail** — the detail view page
- **Member Edit** — the edit form
- **Membership Suspension** — the suspension workflow

Bad sections: "POST /api/members", "PATCH endpoint", "List All Members endpoint"

## Handling Validations

Every validation rule must be expressed as a form interaction scenario:

**Wrong:**
> Address.of() throws IllegalArgumentException when street is blank

**Right:**
> WHEN user submits the registration form with blank street
> THEN the system shows an error message indicating street is required

Group related validations under the relevant form section (registration form, edit form).

## Handling Authorization

Do not write separate "HTTP 401/403" scenarios. Instead express authorization as UI availability:

**Wrong:**
> WHEN unauthenticated request is made to POST /api/members
> THEN HTTP 401 Unauthorized is returned

**Right:**
> WHEN user without MEMBERS:CREATE permission views the member list
> THEN the "Registrovat člena" button is not displayed

## What Does NOT Belong in Specs

Move these to `openspec/specs/non-functional-requirements/spec.md`:

- HTTP status codes (200, 201, 204, 400, 401, 403, 404, 409)
- HAL+FORMS media type and response structure (`_embedded`, `_links`, `_templates`)
- HATEOAS link presence/absence conditions
- ISO-8601 date/datetime serialization format
- Paginated response metadata format (`page.totalElements`, `page.totalPages`)
- JSON field names and response body structure
- Backward compatibility for API clients
- Concurrent update / optimistic locking technical details
- Value object internal implementation (constructor validation, factory method behavior)
- UserId/MemberId type-safety technical scenarios

## What STAYS in Specs (even if technical-sounding)

These are functional requirements expressed from user perspective:

- Validation rules (expressed as form feedback the user sees)
- Permission-based UI visibility (what buttons/sections appear)
- Data formats the user enters (birth number format, IBAN format, phone E.164)
- Warning dialogs and confirmation flows
- Automatic behaviors triggered by user actions (welcome email, age-based reassignment)

## Requirement Structure

```markdown
### Requirement: <capability name>

<1-2 sentences describing the capability from user perspective>

#### Scenario: <user-facing scenario name>

- **WHEN** ...
- **THEN** ...
```

Keep requirement descriptions concise. Avoid implementation language ("The system SHALL store... in the database using AES-256").

## Purpose Section

The `## Purpose` section should describe **what the feature does for users**, not the technical scope:

**Wrong:**
> This specification defines requirements for the REST API endpoints for member management including CRUD operations...

**Right:**
> Covers the full lifecycle of club members: registration, viewing and editing member profiles, and suspending memberships. Defines what information is collected, who can see and edit it, and how the system guides users through each workflow.

## Checklist Before Submitting a Spec

- [ ] Every scenario uses user language (no HTTP verbs, no class names)
- [ ] Organized by UI page/context, not by API endpoint
- [ ] Validations expressed as form feedback
- [ ] Authorization expressed as UI availability (button visible/hidden)
- [ ] No JSON field names or response structure details
- [ ] No HTTP status codes
- [ ] Technical details moved to non-functional-requirements