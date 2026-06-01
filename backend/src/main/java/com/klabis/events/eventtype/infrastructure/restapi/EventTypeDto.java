package com.klabis.events.eventtype.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import com.klabis.events.EventTypeId;

import java.util.Set;

record EventTypeDto(
        @HalForms(access = HalForms.Access.READ_ONLY) EventTypeId id,
        String name,
        String color,
        int sortOrder,
        Set<Integer> orisDisciplineIds
) {
}
