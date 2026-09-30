package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.klabis.members.MemberTestDataBuilder.aMember;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Member JDBC Repository - bornOn filter")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})
)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM members.members")
@ActiveProfiles("test")
class MemberRepositoryBornOnTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("should return only members born on one of the given dates, regardless of status")
    void shouldFilterByBirthDates() {
        memberRepository.save(aMember().withRegistrationNumber("ZBM0801").withDateOfBirth(LocalDate.of(2008, 3, 1)).build());
        memberRepository.save(aMember().withRegistrationNumber("ZBM0802").withDateOfBirth(LocalDate.of(2008, 2, 29)).withActive(false).build());
        memberRepository.save(aMember().withRegistrationNumber("ZBM0901").withDateOfBirth(LocalDate.of(2009, 3, 1)).build());

        List<Member> found = memberRepository.findAll(MemberFilter.all()
                .withBornOn(Set.of(LocalDate.of(2008, 3, 1), LocalDate.of(2008, 2, 29))));

        assertThat(found).extracting(m -> m.getRegistrationNumber().getValue())
                .containsExactlyInAnyOrder("ZBM0801", "ZBM0802");
    }

    @Test
    @DisplayName("empty set of dates matches nobody")
    void emptyDatesMatchNobody() {
        memberRepository.save(aMember().withRegistrationNumber("ZBM0801").withDateOfBirth(LocalDate.of(2008, 3, 1)).build());

        assertThat(memberRepository.findAll(MemberFilter.all().withBornOn(Set.of()))).isEmpty();
    }
}
