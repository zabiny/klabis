# Klabis best practises for backend tests

Testing patterns are documented in the `developer:klabis-backend-patterns` skill (`references/testing-guide.md`).

## Retained here: Mapper Tests scope

Mapper unit tests (Domain → ResponseDTO) test various domain object states and their mapping:
- active vs. suspended aggregate
- adult vs. minor (with/without guardian)
- all optional fields present vs. minimal object

## Retained here: Examples

- `../examples/MemberControllerApiTest.java` — reference @WebMvcTest example
- `../examples/MemberRepositoryTest.java` — reference @DataJdbcTest example