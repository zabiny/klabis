# Change: Define Member-User ID Relationship

## Why

Currently, there's ambiguity in how member and user entities relate to each other regarding their IDs. The system needs
a clear design rule that when a member is created for a user, the user's ID should be used as the member's ID. This
ensures:

- Simplified relationship between User and Member aggregates
- Clear consistency between user identification and member identification
- Reduced complexity in queries and joins between user and member data

## What Changes

- **DEFINE** that every Member SHALL be created with a User, and the User's ID SHALL be used as the Member's ID
- **INTRODUCE** UserId as a Java record in users.domain package to type-safely represent IDs for both User and Member
  aggregates
- **ESTABLISH** the relationship pattern where Member ID = User ID for all members (both are UserId record instances)
- **IMPLEMENT** automatic User creation as part of Member registration flow (no API changes)
- **OPTIMIZE** queries by leveraging shared ID between User and Member aggregates

## Impact

- Affected specs:
    - `members` - Member aggregate ID generation (uses User's UserId)
    - `auth` - User aggregate and its relationship to Member (shared ID)
- Affected code:
    - RegisterMemberCommandHandler (creates User first, then Member with shared UserId)
    - User and Member aggregates (use UserId value object)
    - Repository queries (simplified by shared ID)
    - No API contract changes (internal implementation only)
- Note: No migration needed (in-memory database, no existing members)
