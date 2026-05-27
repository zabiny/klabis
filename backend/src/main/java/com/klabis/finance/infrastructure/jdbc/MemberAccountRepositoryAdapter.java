package com.klabis.finance.infrastructure.jdbc;

import com.klabis.common.pagination.TranslatedPageable;
import com.klabis.common.users.UserId;
import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@SecondaryAdapter
@Repository
@Component
class MemberAccountRepositoryAdapter implements MemberAccountRepository {

    private static final Map<String, String> DOMAIN_TO_DB_COLUMN = Map.of(
            "occurredAt", "occurred_at",
            "recordedAt", "recorded_at",
            "type", "type",
            "amount", "amount"
    );

    private final MemberAccountJdbcRepository jdbcRepository;
    private final NamedParameterJdbcTemplate namedJdbc;

    MemberAccountRepositoryAdapter(MemberAccountJdbcRepository jdbcRepository,
                                   NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcRepository = jdbcRepository;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public MemberAccount save(MemberAccount account) {
        return jdbcRepository.save(MemberAccountMemento.from(account)).toMemberAccount();
    }

    @Override
    public Optional<MemberAccount> findById(MemberId memberId) {
        return jdbcRepository.findById(memberId.uuid()).map(MemberAccountMemento::toMemberAccount);
    }

    @Override
    public Optional<Money> findBalanceById(MemberId memberId) {
        String sql = "SELECT balance_amount, balance_currency FROM member_account WHERE member_id = :memberId";
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        try {
            return Optional.ofNullable(namedJdbc.queryForObject(sql, params, (rs, rowNum) ->
                    Money.of(rs.getBigDecimal("balance_amount"),
                            Currency.getInstance(rs.getString("balance_currency")))));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Transaction> findReversalOf(TransactionId transactionId) {
        String sql = "SELECT * FROM finance_transaction WHERE reverses_transaction_id = :txId";
        MapSqlParameterSource params = new MapSqlParameterSource("txId", transactionId.value());
        List<Transaction> results = namedJdbc.query(sql, params, new TransactionRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean existsById(MemberId memberId) {
        return jdbcRepository.existsById(memberId.uuid());
    }

    @Override
    public Page<Transaction> findTransactions(MemberId memberId, LocalDate occurredAtFrom,
                                              LocalDate occurredAtTo, TransactionType type, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("memberId", memberId.uuid());

        List<String> conditions = new ArrayList<>();
        conditions.add("member_account_id = :memberId");

        if (occurredAtFrom != null) {
            conditions.add("occurred_at >= :occurredAtFrom");
            params.addValue("occurredAtFrom", occurredAtFrom);
        }
        if (occurredAtTo != null) {
            conditions.add("occurred_at <= :occurredAtTo");
            params.addValue("occurredAtTo", occurredAtTo);
        }
        if (type != null) {
            conditions.add("type = :type");
            params.addValue("type", type.name());
        }

        String whereClause = conditions.stream().collect(Collectors.joining(" AND "));
        String countSql = "SELECT COUNT(*) FROM finance_transaction WHERE " + whereClause;

        Pageable dbPageable = TranslatedPageable.translate(pageable, DOMAIN_TO_DB_COLUMN);
        String orderClause = buildOrderClause(dbPageable.getSort());
        String dataSql = "SELECT * FROM finance_transaction WHERE " + whereClause + orderClause
                + " LIMIT " + dbPageable.getPageSize() + " OFFSET " + dbPageable.getOffset();

        long total = Optional.ofNullable(namedJdbc.queryForObject(countSql, params, Long.class)).orElse(0L);
        List<Transaction> transactions = namedJdbc.query(dataSql, params, new TransactionRowMapper());

        return new PageImpl<>(transactions, pageable, total);
    }

    private String buildOrderClause(Sort sort) {
        if (sort.isUnsorted()) {
            return " ORDER BY occurred_at DESC, recorded_at DESC";
        }
        String orders = sort.stream()
                .map(order -> order.getProperty() + " " + order.getDirection().name())
                .collect(Collectors.joining(", "));
        return " ORDER BY " + orders + ", recorded_at DESC";
    }

    private static class TransactionRowMapper implements RowMapper<Transaction> {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID reversesId = (UUID) rs.getObject("reverses_transaction_id");
            return Transaction.reconstruct(
                    new TransactionId(rs.getObject("id", UUID.class)),
                    TransactionType.valueOf(rs.getString("type")),
                    Money.of(rs.getBigDecimal("amount"), Currency.getInstance(rs.getString("currency"))),
                    rs.getString("note"),
                    rs.getTimestamp("recorded_at").toInstant(),
                    rs.getObject("occurred_at", LocalDate.class),
                    new UserId(rs.getObject("recorded_by_user_id", UUID.class)),
                    reversesId != null ? new TransactionId(reversesId) : null
            );
        }
    }
}
