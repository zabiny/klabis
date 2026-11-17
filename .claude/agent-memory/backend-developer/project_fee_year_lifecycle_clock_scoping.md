---
name: project-fee-year-lifecycle-clock-scoping
description: FeeYearLifecycleService owns a Clock (choose/remove window); merged test needs per-nested-class clock overrides — admin fixtures relative to real now, choice fixtures fixed at 2026-03-15
metadata:
  type: project
---

`FeeYearLifecycleService` (membershipfees) owns a `Clock` used by `chooseFeeLevel` and `removeFeeChoice` (and by `assignLevel`'s campaign-closed check). Its test class `FeeYearLifecycleServiceTest` merges the old close/process, admin-assignment and member-choice port tests.

**Why:** the admin-assignment fixtures build campaigns with `LocalDate.now().minusDays(1)` / `plusDays(10)`, so they need the real system clock; the member-choice fixtures are fixed at 2026-03-15 (`CHOICE_TODAY`), so they need a fixed clock. A single class-level clock breaks whichever group is not matched.

**How to apply:** the outer `@BeforeEach` sets `clock = Clock.systemDefaultZone()` and rebuilds `testedInstance` through a `newTestedInstance()` helper; each member-choice nested class (`ChooseFeeLevel`, `RemoveFeeChoice`) overrides it in its own `@BeforeEach` with the fixed clock and re-creates the service. The admin path previously called `LocalDate.now()` directly and now goes through the injected clock — same value in production (`ClockConfiguration` supplies `Clock.systemDefaultZone()`).

**Port split (user decision):** `FeeYearLifecyclePort` keeps exactly the five commands (`closeCampaign`, `processCampaignEnd`, `assignLevel`, `chooseFeeLevel`, `removeFeeChoice`). The read queries `getCurrentChoice` / `getRecommendedLevelForYear` belong to `MemberFeeHistoryPort` / `MemberFeeHistoryService`, which resolves last year's level itself through the group repository; their tests are nested in `MemberFeeHistoryServiceTest`. `MemberFeeChoiceController` therefore injects both ports — the history port for reads, the lifecycle port for writes.

**Scheduler:** `FeeSelectionDeadlineScheduler` also injects the same `Clock` (no test constructs it, so constructor changes are safe).
