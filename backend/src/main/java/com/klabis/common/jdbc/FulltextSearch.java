package com.klabis.common.jdbc;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class FulltextSearch {

    private FulltextSearch() {
    }

    /**
     * Returns ids of rows where every whitespace-separated token of {@code query} matches at least one
     * of {@code columns}, ignoring case and diacritics. {@code query} must be trimmed and non-blank;
     * {@code table} and {@code columns} are concatenated into the SQL and must be constants.
     */
    public static List<UUID> findIds(NamedParameterJdbcTemplate jdbc, String table, String query, String... columns) {
        String[] tokens = query.split("\\s+");
        MapSqlParameterSource params = new MapSqlParameterSource();

        String where = IntStream.range(0, tokens.length)
                .mapToObj(i -> {
                    params.addValue("token" + i, "%" + tokens[i].toLowerCase() + "%");
                    return Arrays.stream(columns)
                            .map(column -> "unaccent(lower(" + column + ")) LIKE unaccent(:token" + i + ")")
                            .collect(Collectors.joining(" OR ", "(", ")"));
                })
                .collect(Collectors.joining(" AND "));

        return jdbc.query("SELECT id FROM " + table + " WHERE " + where, params,
                (rs, rowNum) -> rs.getObject(1, UUID.class));
    }
}
