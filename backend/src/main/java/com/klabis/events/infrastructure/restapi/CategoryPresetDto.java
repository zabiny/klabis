package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import com.klabis.events.CategoryPresetId;

import java.util.List;

record CategoryPresetDto(
        @HalForms(access = HalForms.Access.READ_ONLY) CategoryPresetId id,
        String name,
        List<String> categories
) {
}
