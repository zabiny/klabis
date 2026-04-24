package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Ensures the {@code unaccent} function is available on the current database.
 * <p>
 * On PostgreSQL, this installs the standard {@code unaccent} extension.
 * On H2 (used in tests), it registers an equivalent Java-backed alias that
 * strips diacritical marks using {@link java.text.Normalizer}.
 * <p>
 * Both variants are idempotent: {@code IF NOT EXISTS} / {@code DETERMINISTIC} aliases
 * are safe to re-run.
 */
public class V004__AddUnaccentExtension extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        String productName = meta.getDatabaseProductName();

        if (productName != null && productName.toLowerCase().contains("h2")) {
            registerH2Alias(conn);
        } else {
            installPostgresExtension(conn);
        }
    }

    private void installPostgresExtension(Connection conn) throws Exception {
        conn.createStatement().execute("CREATE EXTENSION IF NOT EXISTS unaccent");
    }

    private void registerH2Alias(Connection conn) throws Exception {
        // Reference a compiled static method so H2 doesn't need `javac` at runtime
        // (JRE-only runtime images don't ship the compiler).
        conn.createStatement().execute(
                "CREATE ALIAS IF NOT EXISTS UNACCENT FOR \"com.klabis.common.jdbc.UnaccentFunction.unaccent\""
        );
    }
}
