package com.klabis.common.ui;

import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.util.Assert;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class HalFormsSupport {

    private static LastInvocationAware getLastInvocationAware(Object invocation) {
        Assert.isInstanceOf(LastInvocationAware.class, invocation);

        return DummyInvocationUtils.getLastInvocationAware(invocation);
    }

    /**
     * Returns affordance for target invocation
     *
     * @param invocation
     * @return
     */
    public static List<Affordance> affordIfAuthorized(Object invocation) {
        LastInvocationAware lastInvocationAware = getLastInvocationAware(invocation);

        Affordance result = afford(lastInvocationAware);

        // update affordance model: if request body is record, change `readOnly` attribute based on @HalForms annotation (if not present, leave original value)

        return List.of(result);
    }

    public static WebMvcLinkBuilder linkToIfAuthorized(Object invocation) {
        return linkTo(invocation);
    }

}
