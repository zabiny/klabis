package org.springframework.hateoas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Link instance which holds full information needed to retrieve "response" from link target. Usable for cases where we would like to embedd target link data into HalResponse with that link to eliminate need to fetch that data separately.
 * TODO: collected embeddable data are not yet serialized into HAL response _embedded list. That needs to be completed.
 */
public class LinkWithEmbeddedResource extends Link {

    private final SimpleMethodInvocation embeddMethodInvocation;

    public LinkWithEmbeddedResource(Link link, LastInvocationAware lastInvocationAware, Object invocationTarget) {
        super(link.getRel(), link.getHref(), link.getHreflang(), link.getMedia(), link.getTitle(),
                link.getType(), link.getDeprecation(), link.getProfile(), link.getName(),
                link.getTemplate(), link.getAffordances());

        Assert.isTrue(lastInvocationAware != null, "LastInvocationAware must not be null");

        embeddMethodInvocation = new SimpleMethodInvocation(invocationTarget,
                lastInvocationAware.getLastInvocation().getMethod(),
                lastInvocationAware.getLastInvocation().getArguments());


        int passedArguments = embeddMethodInvocation.getArguments().length;
        int expectedArguments = embeddMethodInvocation.getMethod().getParameterCount();
        Assert.isTrue(expectedArguments == passedArguments,
                "Last invocation must have been done with all arguments - some arguments are missing. Expecting %d, got %d".formatted(
                        expectedArguments,
                        passedArguments));
    }

    /**
     * @return Returns data from this link's target API. (same data like it would be called from API). Should be possible to use this to embed that value into HalResponse with this link so client will receive that data together with link and will not need to call API to fetch that data.
     */
    @JsonIgnore
    public EmbeddedWrapper getTargetLinkResponse() {
        Object[] arguments = embeddMethodInvocation.getArguments();
        Method method = embeddMethodInvocation.getMethod();
        try {
            Object embedd = method.invoke(embeddMethodInvocation.getThis(), arguments);
            return new EmbeddedWrappers(true).wrap(embedd, getRel());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
