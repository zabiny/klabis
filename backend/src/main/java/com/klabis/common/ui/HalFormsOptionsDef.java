package com.klabis.common.ui;

import org.springframework.hateoas.Link;

import java.util.Collection;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

/**
 * How a HAL-FORMS property's {@code options} should be rendered: a plain value list (the wire
 * format the frontend localises itself, no prompt), value+prompt pairs embedded inline, or a link
 * the client follows to fetch options separately. A single affordance can mix all three kinds
 * across its fields — see {@code HalFormsSupport#klabisAffordWithOptions}.
 */
public sealed interface HalFormsOptionsDef {

    /**
     * Plain value-only options — serialized as {@code options.inline: ["A","B"]} with no
     * valueField/promptField. The frontend localises these values itself, so this wire format
     * must not change to value/prompt pairs.
     */
    record Values(List<String> values) implements HalFormsOptionsDef {
    }

    record Inline(List<HalFormsInlineOption> values) implements HalFormsOptionsDef {
    }

    record Remote(Link link) implements HalFormsOptionsDef {
    }

    /**
     * Builds a {@link Values} from the given value strings.
     */
    static Values values(Collection<String> values) {
        return new Values(List.copyOf(values));
    }

    /**
     * Builds a {@link Remote} pointing at the given {@code methodOn(...)} invocation's self link,
     * e.g. {@code HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions())}.
     */
    static Remote remote(Object invocation) {
        return new Remote(linkTo(invocation).withSelfRel());
    }
}
