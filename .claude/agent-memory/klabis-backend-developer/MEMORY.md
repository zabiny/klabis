# Backend Developer Memory

## Project patterns confirmed

### ResponseEntity.noContent() is a HeadersBuilder, not BodyBuilder
- `ResponseEntity.noContent()` returns `HeadersBuilder<?>`, cannot be assigned to `BodyBuilder`
- To conditionally add headers to 204 responses, use chained approach:
  ```java
  if (!warnings.isEmpty()) {
      return ResponseEntity.noContent().header("X-Warnings", ...).build();
  }
  return ResponseEntity.noContent().build();
  ```

### BirthNumber domain model (Czech birth numbers)
- Month ranges: 01-12 = MALE, 21-32 = MALE (extra), 51-62 = FEMALE, 71-82 = FEMALE (extra)
- Month 71-82 is valid — was not originally handled but added as part of consistency warning task
- `extractDate(birthYear)` uses the caller-provided full birth year (e.g. 1990) to build LocalDate
- `indicatesGender()` returns MALE or FEMALE based on month range

### X-Warnings response header pattern
- Use `response.header("X-Warnings", warnings.toArray(String[]::new))` to set multiple values
- Header is set only when list is non-empty
- Works with both 201 Created and 204 No Content

### Test patterns — MVC static imports
- `patch()` method requires explicit static import: `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;`
- Not included by default when other MockMvcRequestBuilders are imported

### Existing test guard — shouldThrowWhenMonthIsInvalid
- Original test used month 71 as invalid; after adding 71-82 support, month 71 became valid
- Updated test to use month 83 (outside all valid ranges: 01-12, 21-32, 51-62, 71-82)

### Field-level authorization on records (Jackson BeanSerializerModifier)
- See [feedback_field_level_auth_pattern.md](feedback_field_level_auth_pattern.md) for full pattern
- Annotations (`@PreAuthorize`, `@HasAuthority`, `@HandleAuthorizationDenied`) go directly on record components — no interface required
- `FieldSecurityBeanSerializerModifier` + `SecuredBeanPropertyWriter` evaluate auth during Jackson serialization
- `FieldSecurityJacksonModule` registered via `@JsonComponent` — auto-discovered in `@WebMvcTest` and `@SpringBootTest`
- HAL+FORMS template filtering (`HalFormsSupport`) already reads the same record component annotations — no extra wiring needed
