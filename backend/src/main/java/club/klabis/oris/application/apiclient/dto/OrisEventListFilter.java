package club.klabis.oris.application.apiclient.dto;

import club.klabis.oris.application.apiclient.OrisApiClient;

import java.time.LocalDate;

public record OrisEventListFilter(
        String region,
        LocalDate dateFrom,
        LocalDate dateTo
) {
    public static OrisEventListFilter EMPTY = new OrisEventListFilter(null, null, null);

    public static OrisEventListFilter createDefault() {
        return new OrisEventListFilter(OrisApiClient.REGION_JIHOMORAVSKA,
                LocalDate.now(),
                LocalDate.now().plusMonths(3));
    }

    public OrisEventListFilter withRegion(String region) {
        return new OrisEventListFilter(region, dateFrom, dateTo);
    }

    public OrisEventListFilter withDateTo(LocalDate dateTo) {
        return new OrisEventListFilter(region, dateFrom, dateTo);
    }

    public OrisEventListFilter withDateFrom(LocalDate dateFrom) {
        return new OrisEventListFilter(region, dateFrom, dateTo);
    }
}
