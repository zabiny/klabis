# Design: Clarify Test Documentation

## Context

The `RegisterMemberAutoProvisioningTest` test class verifies that when a Member is registered, a corresponding User
account is automatically created with:

- Username = registration number
- Status = PENDING_ACTIVATION
- Auto-generated password hash
- MEMBERS_READ authority

The term "auto-provisioning" refers to this automatic User creation, but this isn't documented in the test class itself.

## Goals / Non-Goals

**Goals:**

- Add concise class-level Javadoc that explains "auto-provisioning" in one sentence
- Ensure test method names clearly indicate what's being tested
- Remove the TODO comment on line 26

**Non-Goals:**

- Refactoring test logic or structure (tests are working correctly)
- Changing test assertions or coverage
- Modifying the RegistrationService implementation

## Decisions

### Decision 1: Javadoc structure

Use a concise, single-paragraph Javadoc format that:

- First sentence: Explains what "auto-provisioning" means
- Second sentence: Describes the key invariant being tested (Member ID = User ID)
- Avoids implementation details unless relevant to understanding

**Rationale:** Concise documentation respects developers' time. The test code itself shows the details; Javadoc should
provide the "why" and "what", not the "how".

### Decision 2: Method name clarity

Current method names are already reasonably clear (`shouldCreateUserWhenMemberRegistered`, etc.). Only minor refinements
needed if names don't immediately convey the test intent.

**Rationale:** Test method names should read like requirements. If they already do, no need to change them.
