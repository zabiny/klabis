package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SecondaryAdapter
@Repository
class YearlyFeeChargeMarkerRepositoryAdapter implements YearlyFeeChargeMarkerRepository {

    private static final Logger log = LoggerFactory.getLogger(YearlyFeeChargeMarkerRepositoryAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    YearlyFeeChargeMarkerRepositoryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByMemberIdAndYear(MemberId memberId, int year) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membershipfees.yearly_fee_charge_marker WHERE member_id = ? AND charge_year = ?",
                Integer.class, memberId.value(), year);
        return count != null && count > 0;
    }

    @Override
    public void markCharged(MemberId memberId, int year) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO membershipfees.yearly_fee_charge_marker (member_id, charge_year, charged_at) VALUES (?, ?, ?)",
                    memberId.value(), year, Instant.now());
        } catch (DataIntegrityViolationException e) {
            log.debug("Yearly fee charge marker already exists for member {} year {} — ignoring duplicate", memberId, year);
        }
    }

    @Override
    public Set<MemberId> findChargedMemberIdsForYear(int year) {
        return jdbcTemplate.queryForList(
                        "SELECT member_id FROM membershipfees.yearly_fee_charge_marker WHERE charge_year = ?",
                        UUID.class, year)
                .stream()
                .map(MemberId::new)
                .collect(Collectors.toSet());
    }
}
