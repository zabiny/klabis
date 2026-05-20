package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarFeedToken;
import com.klabis.calendar.domain.CalendarFeedTokenRepository;
import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class CalendarFeedTokenRepositoryAdapter implements CalendarFeedTokenRepository {

    private final CalendarFeedTokenJdbcRepository jdbcRepository;

    CalendarFeedTokenRepositoryAdapter(CalendarFeedTokenJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public CalendarFeedToken save(CalendarFeedToken token) {
        return jdbcRepository.save(CalendarFeedTokenMemento.from(token)).toDomain();
    }

    @Override
    public Optional<CalendarFeedToken> findByUserId(UserId userId) {
        return jdbcRepository.findById(userId.uuid()).map(CalendarFeedTokenMemento::toDomain);
    }

    @Override
    public List<CalendarFeedToken> findByTokenLookup(String tokenLookup) {
        return jdbcRepository.findByTokenLookup(tokenLookup).stream()
                .map(CalendarFeedTokenMemento::toDomain)
                .toList();
    }
}
