## 1. Member detail shows effective contact (vertical slice)

- [x] 1.1 Add `displayContact` (email/phone with `value`, `fromGuardian`, `guardianName`; marker fields `x-klabis-authority: MEMBERS_MANAGE`) to `MemberDetailsResponse` in `docs/openapi/spec/members.yaml`; regenerate the bundle and frontend types. Verify: generation succeeds and `klabis-full.json` contains the field.
- [x] 1.2 TDD: backend resolution of the effective contact per spec "Effective Contact Of A Member" (own value, first guardian by surname, independent e-mail/phone, no value) and fill it in `getMember`. Verify: unit tests for the resolver and a `@WebMvcTest` of detail for admin vs regular user (marker present/absent); `email`/`phone` stay raw.
- [x] 1.3 TDD: `MemberDetailPage` contact section renders `displayContact`; admin sees "kontakt zákonného zástupce (jméno)" marker, others only the value; add labels to `src/localization/labels.ts`. Verify: vitest for adult, minor with own, minor via guardian as admin and as regular user; `npm run build` passes.
- [x] 1.4 Verify the edit form of a minor without own e-mail leaves the field empty (test on the HAL-FORMS template prefill).

## 2. Member list contact column

- [x] 2.1 Add `displayContact` to `MemberSummaryResponse` (MEMBERS_MANAGE only) and fill it in `listMembers`; regenerate bundle/types. Verify: `@WebMvcTest` of list for admin (present, guardian fallback) and regular user (absent).
- [x] 2.2 TDD: `MembersPage` contact column shows e-mail and telephone as two lines with the guardian marker; replaces the e-mail-only column. Verify: vitest; `npm run build` passes.

## 3. Wrap-up

- [x] 3.1 Manual check on http://localhost:3000 with an example minor lacking own contact (admin `ZBM9000` and member `ZBM9500`). Verify: visual check of detail and list.
- [x] 3.2 Code review of backend and frontend changes before commit.
