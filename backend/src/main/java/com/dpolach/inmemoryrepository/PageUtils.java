package com.dpolach.inmemoryrepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public class PageUtils {

    public static <T> Page<T> create(Collection<T> allItems, Pageable pageable) {

        List<T> sortedItems = allItems.stream().sorted(SortComparator.of(pageable.getSort())).toList();

        return new PageImpl<>(sortedItems.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList(),
                pageable,
                allItems.size());
    }

}
