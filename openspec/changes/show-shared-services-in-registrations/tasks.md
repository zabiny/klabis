## 1. Backend: shared choices in the registration list payload

- [x] 1.1 Write failing `EventControllerTest` cases: `registrationDtoList` rows carry `wantsSharedTransport`/`wantsSharedAccommodation` per offer combination (both offers on, transport only, accommodation only, none) — present with real value when the offer is enabled, property absent when disabled, for a non-coordinator member
- [x] 1.2 Add `wantsSharedTransport` and `wantsSharedAccommodation` (plain `boolean`, description documents per-offer inclusion, no security annotations) to `RegistrationSummaryDto` in `docs/openapi/spec/events.yaml`
- [x] 1.3 Regenerate backend codegen models and implement `RegistrationDtoMapper.toDto` — set each property from the registration when the event's matching offer is enabled, `null` otherwise (NON_NULL drops it)
- [x] 1.4 Run backend tests (test-runner): new cases green, existing event/registration tests unaffected

## 2. Frontend: Ano/Ne columns in the registrations table

- [x] 2.1 Write failing `EventDetailPage.test.tsx` cases: shared transport and shared accommodation columns render "Ano"/"Ne" per registration when the event enables them; each column absent when its offer is disabled; no shared columns when neither offer is enabled
- [x] 2.2 Regenerate frontend types (`npm run openapi`) and implement conditional `TableCell`s in `RegistrationsTable` gated on `event.sharedTransportEnabled`/`event.sharedAccommodationEnabled`, values mapped through `labels.ui.yes`/`labels.ui.no`, headers reusing `labels.fields.sharedTransportEnabled`/`labels.fields.sharedAccommodationEnabled`
- [x] 2.3 Run frontend tests and `npm run build` (test-runner)

## 3. End-to-end verification

- [x] 3.1 QA on http://localhost:3000 with example data: event with both offers shows both columns with correct Ano/Ne per member; event without offers shows none; member (ZBM9500) and admin (ZBM9000) views both correct
- [ ] 3.2 Run `npm run refresh-backend-server-resources` (frontend assets for the backend build) and final code review before commit
