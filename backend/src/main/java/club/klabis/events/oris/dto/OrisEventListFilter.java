package club.klabis.events.oris.dto;

import club.klabis.oris.infrastructure.apiclient.OrisApiClient;

import java.time.LocalDate;

public record OrisEventListFilter(
        String region,
        LocalDate dateFrom,
        LocalDate dateTo,
        boolean officialOnly
) {
    public static OrisEventListFilter EMPTY = new OrisEventListFilter(null, null, null, true);

    public static OrisEventListFilter createDefault() {
        return new OrisEventListFilter(OrisApiClient.REGION_JIHOMORAVSKA,
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(3), true);
    }

    public OrisEventListFilter withRegion(String region) {
        return new OrisEventListFilter(region, dateFrom, dateTo, officialOnly);
    }

    public OrisEventListFilter withDateTo(LocalDate dateTo) {
        return new OrisEventListFilter(region, dateFrom, dateTo, officialOnly);
    }

    public OrisEventListFilter withDateFrom(LocalDate dateFrom) {
        return new OrisEventListFilter(region, dateFrom, dateTo, officialOnly);
    }

    public OrisEventListFilter withOfficialOnly(boolean officialOnly) {
        return new OrisEventListFilter(region, dateFrom, dateTo, officialOnly);
    }
}
