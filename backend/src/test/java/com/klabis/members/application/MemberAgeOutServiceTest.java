package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.MissingDataItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberAgeOutService")
class MemberAgeOutServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberCompletenessPort completenessPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MemberAgeOutService service() {
        return new MemberAgeOutService(memberRepository, completenessPort, eventPublisher);
    }

    @Test
    @DisplayName("should recompute and save completeness and publish event for each member turning 18")
    void shouldRecomputeSaveAndPublish() {
        LocalDate today = LocalDate.now();
        Member member = MemberTestDataBuilder.aMember()
                .withDateOfBirth(today.minusYears(18))
                .withDataIncomplete(false)
                .build();
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of(member));
        when(completenessPort.missingData(member)).thenReturn(Set.of(MissingDataItem.EMAIL));

        service().processMembersComingOfAge(today);

        ArgumentCaptor<MemberFilter> filter = ArgumentCaptor.forClass(MemberFilter.class);
        verify(memberRepository).findAll(filter.capture());
        assertThat(filter.getValue().status()).isEqualTo(MemberFilter.StatusFilter.ALL);
        assertThat(filter.getValue().bornOn()).containsExactly(today.minusYears(18));
        assertThat(member.isDataIncomplete()).isTrue();
        verify(memberRepository).save(member);
        verify(eventPublisher).publishEvent(new MinorAgedOutEvent(member.getId()));
    }

    @Test
    @DisplayName("should do nothing when nobody turns 18")
    void shouldDoNothingWhenNobodyTurns18() {
        when(memberRepository.findAll(any(MemberFilter.class))).thenReturn(List.of());

        service().processMembersComingOfAge(LocalDate.of(2026, 9, 30));

        verify(memberRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("member born on 29 February turns 18 on 1 March in a non-leap year")
    void leapDayBornTurnAdultOnFirstMarchInNonLeapYear() {
        assertThat(MemberAgeOutService.birthDatesTurning18On(LocalDate.of(2026, 3, 1)))
                .containsExactlyInAnyOrder(LocalDate.of(2008, 3, 1), LocalDate.of(2008, 2, 29));
    }

    @Test
    @DisplayName("nobody born on 28 February 2008 turns 18 too early: 28 February 2026 excludes leap-day born")
    void leapDayBornNotAdultOn28FebruaryInNonLeapYear() {
        assertThat(MemberAgeOutService.birthDatesTurning18On(LocalDate.of(2026, 2, 28)))
                .containsExactly(LocalDate.of(2008, 2, 28));
    }

    @Test
    @DisplayName("nobody turns 18 on 29 February")
    void nobodyTurns18OnLeapDayItself() {
        assertThat(MemberAgeOutService.birthDatesTurning18On(LocalDate.of(2028, 2, 29))).isEmpty();
    }

    @Test
    @DisplayName("ordinary day has only the matching birth date")
    void ordinaryDay() {
        assertThat(MemberAgeOutService.birthDatesTurning18On(LocalDate.of(2026, 9, 30)))
                .containsExactly(LocalDate.of(2008, 9, 30));
    }
}
