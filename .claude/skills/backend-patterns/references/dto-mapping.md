# DTO ↔ Domain Mapping

Mapping between generated DTOs and domain types via Spring `ConversionService`. The PATCH overlay
mapper is the one exception — see `rest-controller.md`, "PATCH endpoints".

## DTO ↔ Domain Mapping: `Converter<S,T>` + `ConversionService`

**Canonical pattern for any mapper conversion a controller or exception handler needs to call.** Controllers/handlers never inject a concrete `XyzMapper` type directly — they inject Spring's `ConversionService` and call `conversionService.convert(source, Target.class)`. Each externally-called conversion is its own MapStruct-generated `Converter<S,T>` bean, one interface per conversion (not one per mapper). `MonetaryAmountConverter` (`members.infrastructure.restapi`) is the original precedent; `MemberSummaryConverter`/`MemberDetailsConverter`/`DeactivationReasonConverter` and `BulkSyncResultConverter`/`BulkImportResultConverter` (`events.infrastructure.restapi`) follow the same shape:

```java
@Mapper(config = MapstructSpringMapperConfig.class)
interface MonetaryAmountConverter extends Converter<com.klabis.members.MonetaryAmount, MonetaryAmount> {

    @Override
    MonetaryAmount convert(com.klabis.members.MonetaryAmount source);
}
```

```java
// Controller
private final ConversionService conversionService;

ResponseEntity<MemberDetailsResponse> getMember(...) {
    Member member = managementService.getMemberAndRecordView(...);
    return ResponseEntity.ok(conversionService.convert(member, MemberDetailsResponse.class));
}
```

**Why:** `Converter` beans are auto-discovered by `@WebMvcTest` slices — `WebMvcTypeExcludeFilter` always lets them through regardless of the test's `controllers = {...}` filter — so a test never needs to `@MockitoBean`/`@Import` a mapper just because the controller under test happens to call it. This removed the slice-context coupling that direct mapper injection caused.

**Multi-argument conversions** (e.g. a mapper method that took `(request, currentUserId)`) get a wrapper record so the conversion stays a plain `Converter<S,T>`: `RegisterMemberRequestWithParameters(RegisterMemberRequest request, UserId registeredBy)`, converted via `conversionService.convert(new RegisterMemberRequestWithParameters(request, currentUserId), RegistrationPort.RegisterNewMember.class)`. When the construction logic has real branching (conditional nested objects, several domain factory calls) rather than a field-to-field mapping, write the `Converter` as a plain `@Component implements Converter<S,T>` instead of a MapStruct `@Mapper` — see `RegisterNewMemberConverter`.

**Collection conversions** rely on `ConversionService` element-reuse — never write a hand-rolled `Converter<Set<X>, Set<Y>>`. Use the `TypeDescriptor` overload so the element type survives generic erasure:

```java
conversionService.convert(source,
    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(AuthorityDto.class)),
    TypeDescriptor.collection(Set.class, TypeDescriptor.valueOf(Authority.class)));
```

### Pitfall: never `uses = <OtherMapper>` on a `Converter`

A `Converter`'s `@Mapper` annotation must never declare `uses = SomeInternalMapper.class` to reach nested/shared mapping logic. Because `Converter` beans are visible to **every** `@WebMvcTest` slice app-wide (not just tests for their own controller), a `uses=` dependency on a plain, non-`Converter` `@Mapper` forces every unrelated slice in the whole app to additionally provide that mapper's generated `...Impl` bean (`@Import(SomeInternalMapperImpl.class)`), or the slice fails with `NoSuchBeanDefinitionException` at `mvcConversionService` construction time — a failure with no connection to the controller actually under test.

**Fix:** declare nested/shared mapping methods directly on the `Converter` interface that needs them (MapStruct resolves same-interface methods without `uses=`). If two `Converter`s need the same trivial one-liner (a nested enum mapping, a `@ValueMapping`), duplicate it — cheap, and it removes the cross-bean dependency:

```java
@Mapper(config = MapstructSpringMapperConfig.class)
interface BulkSyncResultConverter extends Converter<com.klabis.events.application.BulkSyncResult, BulkSyncResult> {

    @Override
    BulkSyncResult convert(com.klabis.events.application.BulkSyncResult source);

    @Mapping(target = "eventId", source = "eventId.value")
    EventSyncEntry toDto(com.klabis.events.application.BulkSyncResult.EventSyncEntry entry);

    @ValueMapping(source = "SYNCED", target = "SUCCESS")
    EventImportEntryStatus toDto(com.klabis.events.application.BulkSyncResult.SyncStatus status);
}
```

### Pitfall: no module-specific constructor dependencies on a `Converter`

For the same reason, a `Converter` bean must take no constructor dependencies a foreign slice lacks (ports, repositories). `MemberIdToUuidConverter` and `RegisterNewMemberConverter` are safe because they have none. A mapper that needs such dependencies stays a plain class constructed by its controller — see `SyncStateResponseConverter`.

## Jackson 3 Annotation Changes (Spring Boot 4)

Spring Boot 4 uses Jackson 3, which moved some packages — but Spring Boot wrapper annotations changed names too:
- `@JsonComponent` → `@JacksonComponent` (Spring Boot annotation)
- `@JsonMixin` → `@JacksonMixin` (Spring Boot annotation)
- Core/databind packages: `tools.jackson.core`, `tools.jackson.databind`
- **Exception**: `@JsonCreator`, `@JsonValue`, `@JsonInclude` stay in `com.fasterxml.jackson.annotation` — NOT moved
