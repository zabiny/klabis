package com.klabis.members.legalguardian.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LegalGuardian JDBC persistence")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class LegalGuardianPersistenceTest {

    @Autowired
    private LegalGuardianRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static LegalGuardian guardian(String email) {
        return LegalGuardian.create(new LegalGuardian.CreateLegalGuardian(
                new UserId(UUID.randomUUID()), "Jan", "Novák", email, "+420 777 123 456"));
    }

    @Test
    @DisplayName("round-trips the guardian with audit metadata")
    void roundTrip() {
        LegalGuardian saved = repository.save(guardian("jan@example.com"));

        LegalGuardian found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getFirstName()).isEqualTo("Jan");
        assertThat(found.getLastName()).isEqualTo("Novák");
        assertThat(found.getEmail().value()).isEqualTo("jan@example.com");
        assertThat(found.getPhone().value()).isEqualTo("+420 777 123 456");
        assertThat(found.getAuditMetadata()).isNotNull();
    }

    @Test
    @DisplayName("persists an update of the contact details")
    void persistsUpdate() {
        LegalGuardian saved = repository.save(guardian("jan@example.com"));

        saved.update(new LegalGuardian.UpdateLegalGuardian(null, null, "new@example.com", "+420 601 000 000"));
        repository.save(saved);

        LegalGuardian found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEmail().value()).isEqualTo("new@example.com");
        assertThat(found.getPhone().value()).isEqualTo("+420 601 000 000");
    }

    @Test
    @DisplayName("finds a guardian by e-mail ignoring case")
    void findsByEmailIgnoringCase() {
        LegalGuardian saved = repository.save(guardian("Jan.Novak@Example.com"));

        assertThat(repository.findByEmail("jan.novak@example.COM")).get()
                .extracting(LegalGuardian::getId).isEqualTo(saved.getId());
        assertThat(repository.findByEmail("someone@else.com")).isEmpty();
    }

    @Test
    @DisplayName("finds guardians by ids and lists all")
    void findsByIds() {
        LegalGuardian a = repository.save(guardian("a@example.com"));
        LegalGuardian b = repository.save(guardian("b@example.com"));
        repository.save(guardian("c@example.com"));

        assertThat(repository.findAllByIds(List.of(a.getId(), b.getId())))
                .extracting(LegalGuardian::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("login number series hands out increasing numbers")
    void loginNumberSeriesIncreases() {
        Long first = jdbcTemplate.queryForObject("SELECT nextval('members.legal_guardian_login_number_seq')", Long.class);
        Long second = jdbcTemplate.queryForObject("SELECT nextval('members.legal_guardian_login_number_seq')", Long.class);

        assertThat(second).isEqualTo(first + 1);
    }
}
