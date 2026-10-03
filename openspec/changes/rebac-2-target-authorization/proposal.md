# Proposal

## Why

Klabis authorizes almost everything with global authorities (`MEMBERS:MANAGE`, `EVENTS:MANAGE`, …) plus one hard-wired relation: "the target is me" (`x-klabis-owner-visible`). The club needs permissions that hold only over specific targets and that come from relationships — a legal guardian over their children, owners of a group over its members, and many more to come (registering others for events, choosing a membership fee for a child, …). Bolting each of these onto the current owner check would scatter authorization logic across modules and let the offered actions drift away from the enforced ones.

At the same time, global authorities are frozen into the access token at login, so a revoked permission keeps working until the token expires. Relationship-based permissions change whenever a group changes, so they cannot live in the token at all; global ones should follow the same rule.

This is the second of three ordered changes (`rebac-1-…` → **`rebac-2-…`** → `rebac-3-…`). It introduces the mechanism only; the first relationship-based permission arrives in `rebac-3-member-profile-edit-delegation`.

## What Changes

- An authority can be held **over everything** (granted in the permissions dialog — today's "global" authorities) or **over specific targets** (derived from relationships). "Over everything" includes targets created later.
- An action declares a list of authorities, any of which suffices, evaluated against the action's target: the user passes when they hold one of them over everything or over that particular target. A user holding an authority only over some targets is refused for all others.
- Each authority declares what kind of target it is about and whether it may be held over everything, over specific targets, or both. This replaces the unused `GLOBAL` / `CONTEXT_SPECIFIC` scope classification. Administrator authorities can be held only over everything and therefore can never be delegated.
- Relationship sources are pluggable: any module can contribute "user U holds authority A over targets T" and the results are united.
- **BREAKING (behavior)**: A user's permissions are read from the database for each request instead of from the access token. Granting or revoking a permission takes effect on the user's next request, without logging in again. All authorization decisions within one request use the same set of permissions.
- **BREAKING (API, internal)**: user access tokens no longer carry an `authorities` claim; machine-to-machine (`client_credentials`) tokens keep their scope-derived authorities.
- Offered actions (HAL-FORMS templates and links), visible fields and enforced checks are computed by one shared evaluator, so an offered action is always one the user is allowed to perform.
- Code-level authorization checks scattered in controllers and application services are routed through the same evaluator.
- "The target is me" (`x-klabis-owner-visible`) stays as the rule for a person's access to their own data, alongside the new mechanism.
- OpenAPI extensions: `x-klabis-authority` accepts a list; `x-klabis-target-id: <TYPE>` replaces `x-klabis-owner-id`; new `x-klabis-read-authority` on request fields lets a form show a field as read-only.
- New ADR-010 "Relationship-based authorization".

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `non-functional-requirements`: new requirements on authorization consistency — permissions read per request, one permission set per request, offered actions always match enforced checks, targeted vs. everything grants.
- `member-permissions-dialog`: permission changes made in the dialog take effect for the affected user immediately, without re-login.

## Impact

- **Backend `common`**: `Authority` (new attributes, `Scope` removed), `AuthorizationPolicy`, new authorization snapshot + relationship-source SPI + evaluator, `HasAuthority`/`OwnerId` annotations, `HasAuthorityMethodInterceptor`, `HalFormsSupport`, field security (`FieldSecurityBeanSerializerModifier`, `RequestBodyFieldAuthorizationAdvice`), `KlabisJwtAuthenticationConverter`/`KlabisJwtAuthenticationToken`, `CurrentUserData`.
- **Backend `authorizationserver`**: token customizer stops emitting `authorities` for user grants.
- **Backend modules** with imperative checks: `members` (`MemberController`, `OwnProfileEditRule`, `ManagementService`, `GuardianListAccess`), `events` (`EventController`, `EventManagementService`), `groups` (`TrainingGroupController`), `membershipfees` (`MembershipFeeTierController`).
- **OpenAPI tooling**: `tools/openapi-bundle` (validation of the new extensions), `backend/src/main/openapi-templates` (`api.mustache`, `pojo.mustache`, `pathParams.mustache`), all `docs/openapi/spec/*.yaml` using `x-klabis-owner-id`.
- **Tests**: `WithKlabisMockUser` and security test support.
- **Docs**: `docs/design-decisions.md` (ADR-010), `docs/openapi/spec/README.md`, `backend-patterns` and `klabis-api-spec` skills, developer manual.
- **Frontend**: no change expected (it does not read token authorities; it follows HAL affordances).
