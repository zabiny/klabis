package com.klabis.members.application;

import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.HashSet;
import java.util.Set;

@Service
class MemberAgeOutService implements MemberAgeOutPort {

    private static final Logger log = LoggerFactory.getLogger(MemberAgeOutService.class);

    private static final int ADULT_AGE = 18;

    private final MemberRepository memberRepository;
    private final MemberCompletenessPort completenessPort;
    private final ApplicationEventPublisher eventPublisher;

    MemberAgeOutService(MemberRepository memberRepository,
                        MemberCompletenessPort completenessPort,
                        ApplicationEventPublisher eventPublisher) {
        this.memberRepository = memberRepository;
        this.completenessPort = completenessPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void processMembersComingOfAge(LocalDate today) {
        var members = memberRepository.findAll(
                MemberFilter.all().withBornOn(birthDatesTurning18On(today)));

        for (Member member : members) {
            member.recordMissingData(completenessPort.missingData(member));
            memberRepository.save(member);
            eventPublisher.publishEvent(new MinorAgedOutEvent(member.getId()));
        }
        log.info("Members coming of age on {}: {}", today, members.size());
    }

    /**
     * Someone born on 29 February is treated as turning 18 on 1 March in a non-leap year, the same way
     * {@link java.time.Period} counts age.
     */
    static Set<LocalDate> birthDatesTurning18On(LocalDate today) {
        Set<LocalDate> dates = new HashSet<>();
        boolean leapDay = today.getMonth() == Month.FEBRUARY && today.getDayOfMonth() == 29;
        if (!leapDay) {
            dates.add(today.minusYears(ADULT_AGE));
        }
        int birthYear = today.getYear() - ADULT_AGE;
        if (today.getMonth() == Month.MARCH && today.getDayOfMonth() == 1
                && !Year.isLeap(today.getYear()) && Year.isLeap(birthYear)) {
            dates.add(LocalDate.of(birthYear, Month.FEBRUARY, 29));
        }
        return dates;
    }
}
