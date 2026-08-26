## 1. Preparation

- [x] 1.1 Grep `src/test` for `MapperImpl` imports and `@MockitoBean(... Mapper.class)` on `AuthorityMapper`, `BulkResultMapper`, `MemberMapper` to catalogue all test-side wiring that will need updating
- [x] 1.2 Confirm existing `@WebMvcTest` coverage exists for `PermissionController`, `OrisEventController`, `MemberController`, `RegistrationController` (baseline before refactor)

## 2. Slice: AuthorityMapper -> ConversionService (+ reverse-converter spike)

- [x] 2.1 Spike: verify whether `mapstruct-spring-extensions` can auto-derive the reverse `Converter` for a fully MapStruct-generated pair (`toDto`/`toDomain`); record the finding in design.md's Open Questions
- [x] 2.2 Create `Converter<Authority, AuthorityDto>` bean (config `MapstructSpringMapperConfig`), and `Converter<AuthorityDto, Authority>` bean (hand-written or auto-derived per 2.1 finding)
- [x] 2.3 Remove `toDtoSet`/`toDomainSet` from `AuthorityMapper`; delete `AuthorityMapper` interface entirely once both directions are standalone converters (no internal-only helpers remain)
- [x] 2.4 Update `PermissionController` to inject `ConversionService` instead of `AuthorityMapper`; replace `authorityMapper.toDomainSet(...)` with `conversionService.convert(source, TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(AuthorityDto.class)), TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Authority.class)))`
- [x] 2.5 Update/remove obsolete test-side `AuthorityMapper` wiring found in 1.1
- [x] 2.6 Run `PermissionControllerTest` (and any other affected tests) — confirm behavior unchanged

## 3. Slice: BulkResultMapper -> ConversionService

- [x] 3.1 Create `Converter<BulkSyncResult, BulkSyncResultDto>` (name e.g. `BulkSyncResultConverter`) and `Converter<BulkImportResult, BulkImportResultDto>` (name e.g. `BulkImportResultConverter`), both delegating nested `EventSyncEntry`/`SyncStatus`/`ImportStatus` mappings via internal MapStruct `default`/generated methods as today
- [x] 3.2 Update `OrisEventController` to inject `ConversionService` instead of `BulkResultMapper`; replace both `bulkResultMapper.toDto(result)` call sites with `conversionService.convert(result, TargetDto.class)`
- [x] 3.3 Decide whether `BulkResultMapper` interface can be deleted (if no internal-only helpers remain) or kept as a container for nested mappings
- [x] 3.4 Update/remove obsolete test-side `BulkResultMapper` wiring found in 1.1
- [x] 3.5 Run `OrisEventControllerTest` — confirm behavior unchanged

## 4. Slice: MemberMapper -> ConversionService

- [x] 4.1 Add `RegisterMemberRequestWithParameters(RegisterMemberRequest request, UserId registeredBy)` record in `members.infrastructure.restapi`
- [x] 4.2 Create `Converter<Member, MemberSummaryResponse>`, `Converter<Member, MemberDetailsResponse>`, `Converter<DeactivationReasonDto, DeactivationReason>`, `Converter<RegisterMemberRequestWithParameters, RegistrationPort.RegisterNewMember>` beans
- [x] 4.3 Move/keep `guardianToResponse` and `createPersonalInformation` as internal helpers reachable from the new converters (either as `default` methods on `MemberMapper` referenced via `uses`, or inlined into the relevant converter — pick whichever keeps `MemberMapper` as a coherent internal-only mapper)
- [x] 4.4 Update `MemberController` to inject `ConversionService` instead of `MemberMapper`; replace all 3 call sites (`deactivationReasonToDomain`, `toSummaryResponse` via `Page.map`, `toDetailsResponse`)
- [x] 4.5 Update `RegistrationController` to inject `ConversionService` instead of `MemberMapper`; replace `toRegisterNewMemberCommand(request, registeredBy)` call with `conversionService.convert(new RegisterMemberRequestWithParameters(request, registeredBy), RegistrationPort.RegisterNewMember.class)`
- [x] 4.6 Update/remove obsolete test-side `MemberMapper` wiring found in 1.1 (including `BirthNumberAuditControllerTest`'s `@Import({MemberMapperImpl.class})` pattern)
- [x] 4.7 Run `MemberMappingTests`, `UpdateMemberApiTest`, and other affected `@WebMvcTest`s — confirm behavior unchanged

## 5. Verification and documentation

- [x] 5.1 Run full backend test suite via test-runner skill — confirm no regressions
- [x] 5.2 Update `backend-patterns` skill to document the `Converter<S,T>` + `ConversionService` pattern as the standard way to expose MapStruct mapper conversions to external callers (superseding direct mapper injection)
- [x] 5.3 Code review (backend-developer/code-reviewer agent) before committing
- [x] 5.4 Commit per-slice (sections 2, 3, 4 each as their own commit) plus a final commit for section 5
