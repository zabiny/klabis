package com.klabis.members.legalguardian.infrastructure.jdbc;

import com.klabis.members.legalguardian.application.LoginNumberSequence;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@SecondaryAdapter
@Component
class JdbcLoginNumberSequence implements LoginNumberSequence {

    private static final String PREFIX = "EXT";
    private static final long MAX_NUMBER = 9999;

    private final JdbcTemplate jdbcTemplate;

    JdbcLoginNumberSequence(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String next() {
        Long number = jdbcTemplate.queryForObject("SELECT nextval('members.legal_guardian_login_number_seq')", Long.class);
        if (number == null || number > MAX_NUMBER) {
            throw new IllegalStateException("The series of legal guardian login numbers is exhausted");
        }
        return "%s%04d".formatted(PREFIX, number);
    }
}
