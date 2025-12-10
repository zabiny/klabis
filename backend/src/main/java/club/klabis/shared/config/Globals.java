package club.klabis.shared.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class Globals {

    public static final String KLABIS_ZONE_VALUE = "Europe/Prague";

    public static final ZoneId KLABIS_ZONE = ZoneId.of(KLABIS_ZONE_VALUE);

    public static ZonedDateTime toZonedDateTime(int year, int month, int day) {
        return toZonedDateTime(LocalDate.of(year, month, day));
    }

    public static ZonedDateTime toZonedDateTime(LocalDate localDate) {
        return localDate.atStartOfDay(KLABIS_ZONE);
    }

    public static LocalDate toLocalDate(ZonedDateTime zonedDateTime) {
        return zonedDateTime.withZoneSameInstant(Globals.KLABIS_ZONE).toLocalDate();
    }

}
