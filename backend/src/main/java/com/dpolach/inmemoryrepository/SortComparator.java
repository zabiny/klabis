package com.dpolach.inmemoryrepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

public class SortComparator<T> implements Comparator<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SortComparator.class);

    public static <T> Comparator<T> of(Sort sort) {
        return new SortComparator<>(sort);
    }

    private final Sort sort;

    public SortComparator(Sort sort) {
        this.sort = sort;
    }

    @Override
    public int compare(T o1, T o2) {
        for (Sort.Order order : sort) {
            int result = compareValues(o1, o2, getPropertyName(order));
            if (result != 0) {
                return order.getDirection().isDescending() ? -result : result;
            }
        }
        return 0;
    }

    private static String getPropertyName(Sort.Order order) {
        String propertyName = order.getProperty();
        if (propertyName.startsWith("[")) {
            // sort has from some reason property name in format '["name"]' (string containing [" prefix and "] postfix)
            propertyName = propertyName.substring(2, propertyName.length() - 2);
        }

        if (order.isAscending()) {
            return "%s".formatted(propertyName);
        } else {
            return "%s".formatted(propertyName);
        }
    }

    @SuppressWarnings("unchecked")
    private int compareValues(T o1, T o2, String property) {
        try {
            var field = o1.getClass().getDeclaredField(property);
            if (!Comparable.class.isAssignableFrom(field.getType())) {
                LOG.warn("Cannot sort by field %s from class %s - it doesn't implement Comparable interface".formatted(
                        property,
                        o1.getClass().getCanonicalName()));
                return 0;
            }

            field.setAccessible(true);
            Comparable<Object> value1 = (Comparable<Object>) field.get(o1);
            Comparable<Object> value2 = (Comparable<Object>) field.get(o2);
            if (value1 == null && value2 == null) return 0;
            if (value1 == null) return 1;
            if (value2 == null) return -1;
            return value1.compareTo(value2);

        } catch (Exception e) {
            return 0;
        }
    }
}
