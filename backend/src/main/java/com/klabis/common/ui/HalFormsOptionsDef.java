package com.klabis.common.ui;

import org.springframework.hateoas.Link;

import java.util.List;

/**
 * How a HAL-FORMS property's {@code options} should be rendered: either the full value list
 * embedded inline in the affordance, or a link the client follows to fetch options separately.
 * A single affordance can mix both kinds across its fields — see {@code HalFormsSupport}.
 */
public sealed interface HalFormsOptionsDef {

    record Inline(List<HalFormsInlineOption> values) implements HalFormsOptionsDef {
    }

    record Remote(Link link) implements HalFormsOptionsDef {
    }
}
