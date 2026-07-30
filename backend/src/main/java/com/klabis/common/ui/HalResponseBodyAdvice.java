package com.klabis.common.ui;

import com.klabis.common.mvc.MvcComponent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.HalModelBuilder;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Bridges plain payload DTOs (as required by the generated OpenAPI-spec API interfaces) back into
 * the existing HAL postprocessor pipeline.
 * <p>
 * A controller stores its domain object(s) in {@link HalResponseContext} before returning a plain
 * DTO (or a {@link Page} / {@link Collection} of DTOs). This advice picks the domain back up, wraps the
 * payload in {@link EntityModelWithDomain} (or a {@link PagedModel} / {@link CollectionModel} of the
 * same), and manually runs it through
 * a {@link RepresentationModelProcessorInvoker} built from all {@link RepresentationModelProcessor}
 * beans — the same postprocessors {@code ModelWithDomainPostprocessor} subclasses already implement.
 * <p>
 * Runs strictly after Spring HATEOAS's own {@code RepresentationModelProcessorHandlerMethodReturnValueHandler},
 * which only recognizes return values that are already a {@link RepresentationModel}. A plain DTO
 * never satisfies that check, so postprocessing is skipped there entirely — this advice is what
 * makes postprocessing happen for endpoints that return plain payloads. Controllers that still
 * build their own {@code EntityModel}/{@code PagedModel} in the old style are untouched: without an
 * entry in {@link HalResponseContext}, this advice passes the body through unchanged.
 */
@MvcComponent
@ControllerAdvice
public class HalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final List<RepresentationModelProcessor<?>> processors;
    private final PagedResourcesAssembler<Object> pagedResourcesAssembler;

    public HalResponseBodyAdvice(List<RepresentationModelProcessor<?>> processors,
                                  PagedResourcesAssembler<Object> pagedResourcesAssembler) {
        this.processors = processors;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        // An error path (ProblemDetail) may run after a controller already stored its domain object,
        // so the context is cleared unconditionally and the error body passed through untouched.
        if (body instanceof ProblemDetail) {
            HalResponseContext.clear();
            return body;
        }
        if (!isHalMediaType(selectedContentType)) {
            HalResponseContext.clear();
            return body;
        }
        if (body instanceof Page<?> page) {
            return wrapPage(page);
        }
        if (body instanceof Collection<?> collection) {
            return wrapCollection(collection);
        }
        return wrapSingle(body);
    }

    private static boolean isHalMediaType(MediaType selectedContentType) {
        return selectedContentType != null
               && (MediaTypes.HAL_JSON.isCompatibleWith(selectedContentType)
                   || MediaTypes.HAL_FORMS_JSON.isCompatibleWith(selectedContentType));
    }

    private Object wrapSingle(Object body) {
        Object domain = HalResponseContext.takeDomain();
        if (domain == null || body instanceof RepresentationModel<?>) {
            return body;
        }

        HalResponseContext.Embedded embedded = HalResponseContext.takeEmbedded();
        EntityModel<Object> model = HalFormsSupport.entityModelWithDomain(body, domain);
        Object processed = new RepresentationModelProcessorInvoker(processors).invokeProcessorsFor(model);
        return applyEmbeddeds(processed, embedded);
    }

    /**
     * Renders any {@code _embedded} collections a postprocessor declared via
     * {@link EntityModelWithDomain#embed}. Deferred to here because {@code HalModelBuilder} returns a
     * wrapper around the model rather than the model itself, which a
     * {@link RepresentationModelProcessor} cannot return in place of its own input type.
     * <p>
     * Runs after the processors so the links and affordances they added are already on the model the
     * builder wraps; {@code HalRepresentationModel} extends {@code EntityModel} and delegates
     * {@code getContent()} to it, so the payload and its {@code _links} render unchanged.
     */
    private Object applyEmbeddeds(Object processed, HalResponseContext.Embedded embedded) {
        if (embedded == null) {
            return processed;
        }
        return HalModelBuilder.halModelOf(processed)
                .embed(embedded.collection(), embedded.itemType())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Object wrapPage(Page<?> dtoPage) {
        List<?> domainList = HalResponseContext.takeDomainList();
        if (domainList == null) {
            return dtoPage;
        }

        List<?> dtoContent = dtoPage.getContent();
        if (dtoContent.size() != domainList.size()) {
            throw new IllegalStateException(
                    "HAL response advice: DTO page content (%d items) and domain list (%d items) must pair 1:1"
                            .formatted(dtoContent.size(), domainList.size()));
        }

        Page<Object> objectPage = (Page<Object>) dtoPage;
        var domainIterator = domainList.iterator();

        // Plain entityModelWithDomain here, without invoking processors yet — invokeProcessorsFor(pagedModel)
        // below already recurses into each item exactly once (Spring HATEOAS's own CollectionModel handling).
        // Invoking processors here too would run them twice per item (duplicate self links, affordances, ...).
        PagedModel<EntityModel<Object>> pagedModel = pagedResourcesAssembler.toModel(objectPage,
                dto -> HalFormsSupport.entityModelWithDomain(dto, domainIterator.next()));

        // The controller method already ran (and passed authorization) for the exact request
        // parameters below, so re-deriving the self link from them needs no separate authorization
        // check — unlike affordances to *other* endpoints, which still go through klabisAfford in
        // a postprocessor. PagedResourcesAssembler always adds a self link, so mapLink always finds
        // one to replace here.
        currentServletRequest().ifPresent(servletRequest -> {
            Link selfLink = Link.of(buildSelfLinkUri(servletRequest), IanaLinkRelations.SELF);
            pagedModel.mapLink(IanaLinkRelations.SELF, oldLink -> selfLink);
        });

        return new RepresentationModelProcessorInvoker(processors).invokeProcessorsFor(pagedModel);
    }

    private Object wrapCollection(Collection<?> dtoCollection) {
        List<?> domainList = HalResponseContext.takeDomainList();
        if (domainList == null) {
            return dtoCollection;
        }

        if (dtoCollection.size() != domainList.size()) {
            throw new IllegalStateException(
                    "HAL response advice: DTO collection (%d items) and domain list (%d items) must pair 1:1"
                            .formatted(dtoCollection.size(), domainList.size()));
        }

        var domainIterator = domainList.iterator();
        // Plain entityModelWithDomain here, without invoking processors yet — invokeProcessorsFor(collectionModel)
        // below already recurses into each item exactly once, same as in wrapPage.
        List<EntityModel<Object>> items = dtoCollection.stream()
                .map(dto -> HalFormsSupport.entityModelWithDomain(dto, domainIterator.next()))
                .toList();

        CollectionModel<EntityModel<Object>> collectionModel = CollectionModel.of(items);

        // Unlike wrapPage, PagedResourcesAssembler is not involved and adds no self link, so it is
        // built here from the current request — see buildSelfLinkUri for why no authorization check
        // applies to the self link itself.
        currentServletRequest().ifPresent(servletRequest ->
                collectionModel.add(Link.of(buildSelfLinkUri(servletRequest), IanaLinkRelations.SELF)));

        return new RepresentationModelProcessorInvoker(processors).invokeProcessorsFor(collectionModel);
    }

    /**
     * Built from {@code getParameterMap()} rather than {@code getQueryString()}: the latter is
     * {@code null} for {@code MockHttpServletRequest} built via {@code MockMvcRequestBuilders.param(...)}
     * (as opposed to a literal {@code ?query} in the request URL) — a MockMvc-only gap, but the
     * parameter map works identically for both real servlet requests and MockMvc ones.
     * <p>
     * {@code getRequestURI()} returns the raw (percent-encoded) path while {@code getParameterMap()}
     * returns decoded values — decoding the path first lets a single {@code UriComponentsBuilder.encode()}
     * apply consistently to both, instead of double-encoding the path or leaving parameter values
     * (which may themselves contain {@code &}, {@code =}, {@code +}, {@code #}) unescaped in the query.
     */
    private static String buildSelfLinkUri(HttpServletRequest servletRequest) {
        String decodedPath = UriUtils.decode(servletRequest.getRequestURI(), StandardCharsets.UTF_8);
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(decodedPath);
        Map<String, String[]> sortedParams = new TreeMap<>(servletRequest.getParameterMap());
        sortedParams.forEach(builder::queryParam);
        return builder.build().encode().toUriString();
    }

    private static Optional<HttpServletRequest> currentServletRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        return Optional.of(attributes.getRequest());
    }
}
