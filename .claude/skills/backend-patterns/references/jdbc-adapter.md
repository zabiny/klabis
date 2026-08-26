# JDBC Infrastructure Adapter

Persistence via the memento pattern: a `@Table` memento class carries the flattened row, the
repository adapter implements the domain port, and Spring Data provides the query interface.


## Memento Class

```java
@Table("members")
class MemberMemento implements Persistable<UUID> {

    @Id @Column("id") private UUID id;

    // Flattened value objects — no nested objects in DB
    @Column("first_name") private String firstName;
    @Column("email") private String email;
    @Column("street") private String street;   // from Address VO

    // Audit (Spring Data auditing)
    @CreatedDate @Column("created_at") private Instant createdAt;
    @LastModifiedDate @Column("modified_at") private Instant lastModifiedAt;
    @Version @Column("version") private Long version;

    @Transient private Member member;   // Domain reference for event delegation
    @Transient private boolean isNew = true;

    // Domain → Memento (save path)
    public static MemberMemento from(Member member) {
        MemberMemento m = new MemberMemento();
        m.id = member.getId().value();
        m.firstName = member.getPersonalInformation().getName().firstName();
        m.email = member.getEmail().value();
        m.member = member;
        m.isNew = (member.getAuditMetadata() == null);
        return m;
    }

    // Memento → Domain (load path) via Member.reconstruct()
    public Member toMember() {
        return Member.reconstruct(new MemberId(this.id), ...);
    }

    // Domain event delegation (Spring Modulith mechanism)
    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.member != null ? this.member.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) this.member.clearDomainEvents();
    }

    @Override
    public boolean isNew() { return this.isNew; }
}
```

## Repository Adapter

```java
@SecondaryAdapter
@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJdbcRepository jdbcRepository;

    @Override
    public Member save(Member member) {
        return jdbcRepository.save(MemberMemento.from(member)).toMember();
    }

    @Override
    public Optional<Member> findById(MemberId id) {
        return jdbcRepository.findById(id.value()).map(MemberMemento::toMember);
    }
}
```

## Spring Data Repository

```java
@Repository
interface MemberJdbcRepository extends
        CrudRepository<MemberMemento, UUID>,
        PagingAndSortingRepository<MemberMemento, UUID> {

    Optional<MemberMemento> findByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(*) FROM members WHERE ...")
    int countByBirthYear(@Param("birthYear") int birthYear);
}
```
