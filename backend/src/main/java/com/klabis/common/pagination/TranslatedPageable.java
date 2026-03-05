package com.klabis.common.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

/**
 * A translated {@link Pageable} decorator that wraps pagination parameters with translated sort orders.
 * <p>
 * This class implements the Decorator pattern to translate Pageable sort property names from one
 * naming convention to another (e.g., domain properties → database columns for SQL ORDER BY clauses).
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * // In repository adapter
 * private static final Map<String, String> DOMAIN_TO_DB = Map.of(
 *     "eventDate", "event_date",
 *     "userName", "user_name"
 * );
 *
 * Pageable pageable = TranslatedPageable.translate(originalPageable, DOMAIN_TO_DB);
 * }</pre>
 * <p>
 * The translation uses a fail-safe approach: if a property name is not found in the translation map,
 * the original property name is used. This prevents runtime errors for unmapped properties.
 */
public record TranslatedPageable(int page, int size, Sort sort) implements Pageable {

    /**
     * Translates a Pageable's sort property names using the provided mapping.
     * <p>
     * Pagination parameters (page, size) are preserved unchanged.
     * Only the sort orders' property names are translated.
     *
     * @param pageable the original pageable with untranslated sort properties
     * @param propertyNameMap mapping from source property names to target property names
     * @return new TranslatedPageable with translated sort properties
     */
    public static TranslatedPageable translate(Pageable pageable, Map<String, String> propertyNameMap) {
        Sort translatedSort = translateSort(pageable.getSort(), propertyNameMap);
        return new TranslatedPageable(pageable.getPageNumber(), pageable.getPageSize(), translatedSort);
    }

    /**
     * Translates sort property names using the provided mapping.
     *
     * @param sort the original sort with untranslated property names
     * @param propertyNameMap mapping from source property names to target property names
     * @return new Sort with translated property names
     */
    private static Sort translateSort(Sort sort, Map<String, String> propertyNameMap) {
        List<Sort.Order> orders = sort.stream()
                .map(order -> {
                    String translatedProperty = propertyNameMap.getOrDefault(
                            order.getProperty(),
                            order.getProperty()
                    );
                    return new Sort.Order(
                            order.getDirection(),
                            translatedProperty,
                            order.getNullHandling()
                    );
                })
                .toList();

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    @Override
    public int getPageNumber() {
        return page;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return (long) page * size;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new TranslatedPageable(getPageNumber() + 1, getPageSize(), getSort());
    }

    @Override
    public Pageable previousOrFirst() {
        return getPageNumber() == 0 ? this : new TranslatedPageable(getPageNumber() - 1, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return getPageNumber() > 0;
    }

    @Override
    public Pageable first() {
        return new TranslatedPageable(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new TranslatedPageable(pageNumber, getPageSize(), getSort());
    }

    /**
     * Note: This method is not part of the Pageable interface in all Spring Data versions.
     * Use with caution as it may not be called by framework code.
     *
     * @param sort new sort order
     * @return new Pageable with updated sort
     */
    public Pageable withSort(Sort sort) {
        return new TranslatedPageable(getPageNumber(), getPageSize(), sort);
    }

    @Override
    public boolean isPaged() {
        return true;
    }

    @Override
    public boolean isUnpaged() {
        return false;
    }
}
