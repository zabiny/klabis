package com.klabis.common.ui;

import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import com.klabis.common.users.HasAuthority;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.AffordanceModelFactory;
import org.springframework.hateoas.mediatype.ConfiguredAffordance;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.core.MethodInvocation;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.klabis.common.ui.CollectionPropertyContext.markCollectionProperty;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class HalFormsSupport {

    private static HalFormsSupport INSTANCE;

    private final ObjectProvider<OwnershipResolver> ownershipResolverProvider;

    public HalFormsSupport(ObjectProvider<OwnershipResolver> ownershipResolverProvider) {
        this.ownershipResolverProvider = ownershipResolverProvider;
    }

    // TODO: this static causes troubles in tests (some tests are not starting full context and this post construct may not be called yet, so tests are working without ownership resolver)
    // (try to find out way how to do this resolved directly from data)
    @PostConstruct
    void init() {
        INSTANCE = this;
    }

    @PreDestroy
    void destroy() {
        if (INSTANCE == this) {
            INSTANCE = null;
        }
    }

    /**
     * Test hook: several cached test contexts share one JVM, so the instance registered last by
     * {@link #init()} does not have to belong to the context the current test runs against.
     */
    static void useInstance(HalFormsSupport instance) {
        INSTANCE = instance;
    }

    public static Optional<WebMvcLinkBuilder> klabisLinkTo(Object invocation) {
        LastInvocationAware lastInvocationAware = getLastInvocationAware(invocation);

        if (INSTANCE != null && !INSTANCE.isMethodAuthorized(lastInvocationAware)) {
            return Optional.empty();
        }

        return Optional.of(linkTo(invocation));
    }

    public static List<Affordance> klabisAfford(Object invocation) {
        LastInvocationAware lastInvocationAware = getLastInvocationAware(invocation);

        if (INSTANCE != null && !INSTANCE.isMethodAuthorized(lastInvocationAware)) {
            return Collections.emptyList();
        }

        Affordance result = afford(lastInvocationAware);

        // update affordance model: if request body is record, change `readOnly` attribute based on @HalForms annotation (if not present, leave original value)
        Affordance modifiedResult = modifyAffordanceForHalForms(result, lastInvocationAware, Map.of());

        return List.of(modifiedResult);
    }

    /**
     * Like {@link #klabisAfford}, but publishes the affordance under a different template name.
     * <p>
     * Used where one operation serves two distinct intents in the UI — {@code removeGroupMember} is an
     * owner's action on someone else in one place and the caller's own "leave this group" in another,
     * and the client renders the two differently. Only the name changes; field-level authorization and
     * {@code @HalForms} handling are applied exactly as in {@link #klabisAfford}.
     */
    public static List<Affordance> klabisAffordAs(String templateName, Object invocation) {
        LastInvocationAware lastInvocationAware = getLastInvocationAware(invocation);

        if (INSTANCE != null && !INSTANCE.isMethodAuthorized(lastInvocationAware)) {
            return Collections.emptyList();
        }

        Affordance original = modifyAffordanceForHalForms(afford(lastInvocationAware), lastInvocationAware, Map.of());
        Optional<AffordanceModelFactory> halFormsFactoryOpt = getHalFormsModelFactory();
        if (halFormsFactoryOpt.isEmpty()) {
            return List.of(original);
        }
        AffordanceModelFactory halFormsFactory = halFormsFactoryOpt.get();

        Map<MediaType, AffordanceModel> renamed = new HashMap<>();
        getModelsFromAffordance(original).forEach((mediaType, model) -> {
            if (isHalFormsModel(model)) {
                renamed.put(mediaType, halFormsFactory.getAffordanceModel(
                        new RenamedAffordance(templateName, model)));
            } else {
                renamed.put(mediaType, model);
            }
        });

        return List.of(new Affordance(renamed));
    }

    public static <T, D> EntityModel<T> entityModelWithDomain(T dto, D domain) {
        return new EntityModelWithDomain<>(dto, domain);
    }

    private static final String OPTIONS_DEF_REQUEST_ATTR = HalFormsSupport.class.getName() + ".optionsDef";

    /**
     * Returns the options definition (value list, inline value+prompt pairs, or remote link) for the
     * named property from the current request context, or null if none are set.
     */
    static HalFormsOptionsDef getOptionsDefForProperty(String propertyName) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Map<String, HalFormsOptionsDef> ctx = getOptionsDefContext(request);
        return ctx != null ? ctx.get(propertyName) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, HalFormsOptionsDef> getOptionsDefContext(HttpServletRequest request) {
        return (Map<String, HalFormsOptionsDef>) request.getAttribute(OPTIONS_DEF_REQUEST_ATTR);
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Like {@link #klabisAfford}, but injects HAL-FORMS options for the given properties — a plain
     * value list, inline value+prompt pairs, or a link the client follows to fetch options separately
     * (see {@link HalFormsOptionsDef}). A single affordance can mix all three kinds across its fields.
     * Options are stored as request attributes and are automatically scoped to the current HTTP request.
     */
    public static List<Affordance> klabisAffordWithOptions(Object invocation, Map<String, HalFormsOptionsDef> optionsDef) {
        LastInvocationAware lastInvocationAware = getLastInvocationAware(invocation);

        if (INSTANCE != null && !INSTANCE.isMethodAuthorized(lastInvocationAware)) {
            return Collections.emptyList();
        }

        Affordance result = afford(lastInvocationAware);
        Affordance modifiedResult = modifyAffordanceForHalForms(result, lastInvocationAware, optionsDef);
        return List.of(modifiedResult);
    }

    private record MethodAuthMeta(HasAuthority hasAuthority, OwnerVisible ownerVisible, int ownerIdParamIndex) {
        boolean hasSecurityAnnotations() {
            return hasAuthority != null || ownerVisible != null;
        }
    }

    /**
     * Keyed by {@code Method} alone even though the cached value also depends on {@code targetClass}.
     * That is only sound while every caller derives {@code targetClass} from the method itself
     * (today: {@code method.getDeclaringClass()}). A second call site passing an unrelated class —
     * a CGLIB proxy type, say — would silently receive the entry computed for the first caller.
     */
    private static final ConcurrentHashMap<Method, MethodAuthMeta> METHOD_AUTH_CACHE = new ConcurrentHashMap<>();

    private static MethodAuthMeta resolveMethodAuthMeta(Method method, Class<?> targetClass) {
        return METHOD_AUTH_CACHE.computeIfAbsent(method, m -> {
            HasAuthority ha = MethodSecurityAnnotations.findMethodAnnotation(m, targetClass, HasAuthority.class);
            OwnerVisible ov = MethodSecurityAnnotations.findMethodAnnotation(m, targetClass, OwnerVisible.class);
            int ownerIdx = ov != null
                    ? MethodSecurityAnnotations.findAnnotatedParameterIndex(m, targetClass, OwnerId.class)
                    : -1;
            return new MethodAuthMeta(ha, ov, ownerIdx);
        });
    }

    private boolean isMethodAuthorized(LastInvocationAware invocation) {
        MethodInvocation methodInvocation = invocation.getLastInvocation();
        Method method = methodInvocation.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        MethodAuthMeta meta = resolveMethodAuthMeta(method, targetClass);

        if (!meta.hasSecurityAnnotations()) {
            return true;
        }

        org.springframework.security.core.Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (meta.hasAuthority() != null && SecuritySpelEvaluator.hasAuthority(authentication,
                meta.hasAuthority().value())) {
            return true;
        }

        if (meta.ownerVisible() != null && meta.ownerIdParamIndex() >= 0) {
            Object[] arguments = methodInvocation.getArguments();
            Object ownerIdValue = (arguments != null && meta.ownerIdParamIndex() < arguments.length)
                    ? arguments[meta.ownerIdParamIndex()] : null;
            if (ownerIdValue != null) {
                OwnershipResolver resolver = ownershipResolverProvider.getIfAvailable();
                if (resolver != null) {
                    return resolver.isOwner(ownerIdValue, authentication);
                }
            }
        }

        return false;
    }

    private static LastInvocationAware getLastInvocationAware(Object invocation) {
        Assert.isInstanceOf(LastInvocationAware.class, invocation);

        return DummyInvocationUtils.getLastInvocationAware(invocation);
    }


    private static Optional<AffordanceModelFactory> getHalFormsModelFactory() {
        return SpringFactoriesLoader.loadFactories(AffordanceModelFactory.class, HalFormsSupport.class.getClassLoader())
                .stream().filter(f -> MediaTypes.HAL_FORMS_JSON.equals(f.getMediaType())).findFirst();
    }

    /**
     * Modifies affordance to apply @HalForms annotations from record components
     */
    private static Affordance modifyAffordanceForHalForms(Affordance affordance, LastInvocationAware invocation,
                                                           Map<String, HalFormsOptionsDef> optionsDef) {
        // Get method metadata
        MethodInvocation methodInvocation = invocation.getLastInvocation();
        Method method = methodInvocation.getMethod();
        Parameter[] parameters = method.getParameters();

        // Find @RequestBody parameter
        for (Parameter param : parameters) {
            if (param.isAnnotationPresent(RequestBody.class)) {
                Class<?> requestBodyType = param.getType();

                // Check if it's a record
                if (requestBodyType.isRecord()) {
                    return createModifiedAffordance(affordance, optionsDef);
                }
            }
        }

        return affordance;
    }

    /**
     * Creates new affordance using AffordanceModelFactory with modified InputPayloadMetadata
     */
    private static Affordance createModifiedAffordance(Affordance original,
                                                        Map<String, HalFormsOptionsDef> optionsDef) {
        Optional<AffordanceModelFactory> halFormsFactoryOpt = getHalFormsModelFactory();

        if (halFormsFactoryOpt.isEmpty()) {
            return original; // No factory available, return original
        }

        AffordanceModelFactory halFormsFactory = halFormsFactoryOpt.get();

        // Need to get the original MediaType mapping from Affordance
        Map<MediaType, AffordanceModel> originalModels = getModelsFromAffordance(original);

        Map<MediaType, AffordanceModel> newModels = new HashMap<>();

        for (Map.Entry<MediaType, AffordanceModel> entry : originalModels.entrySet()) {
            MediaType mediaType = entry.getKey();
            AffordanceModel model = entry.getValue();

            // For HAL-FORMS models, use our modified version
            if (isHalFormsModel(model)) {
                ConfiguredAffordance configured = new HalFormsConfiguredAffordance(model, optionsDef);
                AffordanceModel newModel = halFormsFactory.getAffordanceModel(configured);
                newModels.put(mediaType, newModel);
            } else {
                // For other media types, keep the original model
                newModels.put(mediaType, model);
            }
        }

        return new Affordance(newModels);
    }

    private static boolean isHalFormsModel(AffordanceModel model) {
        return model.getClass().getSimpleName().contains("HalForms");
    }

    /**
     * Extract the models map from Affordance using reflection (since getModels() is package-private)
     */
    private static Map<MediaType, AffordanceModel> getModelsFromAffordance(Affordance affordance) {
        try {
            java.lang.reflect.Field modelsField = Affordance.class.getDeclaredField("models");
            modelsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<MediaType, AffordanceModel> models = (Map<MediaType, AffordanceModel>) modelsField.get(affordance);

            return models;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract models from affordance", e);
        }
    }

    /**
     * ConfiguredAffordance that delegates everything to an existing model. Subclasses override only the
     * template name or the input metadata, so the two variations cannot drift apart.
     */
    private static class DelegatingConfiguredAffordance implements ConfiguredAffordance {

        protected final AffordanceModel delegate;

        private DelegatingConfiguredAffordance(AffordanceModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getNameOrDefault() {
            return delegate.getName();
        }

        @Override
        public Link getTarget() {
            return delegate.getLink();
        }

        @Override
        public HttpMethod getMethod() {
            return delegate.getHttpMethod();
        }

        @Override
        public AffordanceModel.InputPayloadMetadata getInputMetadata() {
            return delegate.getInput();
        }

        @Override
        public List<QueryParameter> getQueryParameters() {
            return delegate.getQueryMethodParameters();
        }

        @Override
        public AffordanceModel.PayloadMetadata getOutputMetadata() {
            return delegate.getOutput();
        }
    }

    /**
     * Publishes the affordance under the caller's template name instead of the one derived from the
     * controller method, for when one operation serves two distinct intents in the UI.
     */
    private static class RenamedAffordance extends DelegatingConfiguredAffordance {

        private final String templateName;

        private RenamedAffordance(String templateName, AffordanceModel delegate) {
            super(delegate);
            this.templateName = templateName;
        }

        @Override
        public String getNameOrDefault() {
            return templateName;
        }
    }

    /**
     * ConfiguredAffordance wrapper that modifies InputPayloadMetadata based on @HalForms annotations
     */
    private static class HalFormsConfiguredAffordance extends DelegatingConfiguredAffordance {

        private final HalFormsInputPayloadMetadata modifiedInput;

        private HalFormsConfiguredAffordance(AffordanceModel delegate,
                                             Map<String, HalFormsOptionsDef> optionsDef) {
            super(delegate);
            this.modifiedInput = new HalFormsInputPayloadMetadata(delegate.getInput(), optionsDef);
        }

        @Override
        public AffordanceModel.InputPayloadMetadata getInputMetadata() {
            return modifiedInput;
        }
    }

    /**
     * InputPayloadMetadata wrapper that modifies PropertyMetadata based on @HalForms annotations.
     * Inline options are stored in request attributes so they remain available during Jackson serialization
     * without relying on ThreadLocal. Request attributes are automatically scoped to the HTTP request lifecycle.
     */
    private static class HalFormsInputPayloadMetadata implements AffordanceModel.InputPayloadMetadata {
        private static final Logger LOG = LoggerFactory.getLogger(KlabisHalFormsPropertyMetadataWrapper.class);

        private final AffordanceModel.InputPayloadMetadata inputPayloadMetadata;
        private final Map<String, HalFormsOptionsDef> optionsDef;

        HalFormsInputPayloadMetadata(AffordanceModel.InputPayloadMetadata inputPayloadMetadata,
                                     Map<String, HalFormsOptionsDef> optionsDef) {
            this.inputPayloadMetadata = inputPayloadMetadata;
            this.optionsDef = Map.copyOf(optionsDef);
        }

        @Override
        public Stream<AffordanceModel.PropertyMetadata> stream() {
            HttpServletRequest request = currentRequest();
            if (request != null && !optionsDef.isEmpty()) {
                // Jackson materializes every affordance's property list before it writes any of them to JSON,
                // so multiple klabisAffordWithOptions calls on the same response all run their stream()
                // before the first property's "options" is actually serialized. Overwriting the attribute here
                // would make only the last-processed affordance's options visible; merging keeps all of them.
                request.setAttribute(OPTIONS_DEF_REQUEST_ATTR, mergeOptions(
                        getOptionsDefContext(request), optionsDef));
            }
            // Modify property metadata stream based on @HalForms annotations
            return inputPayloadMetadata.stream()
                    .map(this::wrapPropertyMetadata)
                    .filter(this::isPropertyDisplayed);
        }

        private AffordanceModel.PropertyMetadata wrapPropertyMetadata(AffordanceModel.PropertyMetadata metadata) {
            boolean isPayloadClassRecord = inputPayloadMetadata.getType() != null && inputPayloadMetadata.getType()
                    .isRecord();

            AffordanceModel.PropertyMetadata wrapped = getAnnotatedElementForProperty(inputPayloadMetadata, metadata)
                    .map(annotatedElement -> (AffordanceModel.PropertyMetadata) new KlabisHalFormsPropertyMetadataWrapper(
                            metadata,
                            annotatedElement,
                            isPayloadClassRecord,
                            isPropertyAuthorized(inputPayloadMetadata.getType(), metadata.getName())))
                    .orElse(metadata);

            if (wrapped instanceof KlabisHalFormsPropertyMetadataWrapper wrapper && wrapper.isCollectionType()) {
                markCollectionProperty(metadata.getName());
            }

            return wrapped;
        }

        private static <V> Map<String, V> mergeOptions(Map<String, V> existing, Map<String, V> additional) {
            if (existing == null || existing.isEmpty()) {
                return additional;
            }
            Map<String, V> merged = new HashMap<>(existing);
            merged.putAll(additional);
            return merged;
        }

        private boolean isPropertyDisplayed(AffordanceModel.PropertyMetadata propertyMetadata) {
            if (propertyMetadata instanceof KlabisHalFormsPropertyMetadataWrapper wrapper) {
                return wrapper.isDisplayed();
            }
            return true;
        }

        /**
         * Checks whether the current user is authorized to see the given property in the template.
         * <p>
         * For record types, looks for @PreAuthorize or @HasAuthority directly on the record component accessor.
         * For non-record types, looks on interface methods matching the property name —
         * the same annotations that control JSON field visibility for the response DTO.
         */
        private static boolean isPropertyAuthorized(Class<?> payloadType, String propertyName) {
            if (payloadType == null) {
                return true;
            }
            if (payloadType.isRecord()) {
                return Arrays.stream(payloadType.getRecordComponents())
                        .filter(c -> c.getName().equals(propertyName))
                        .filter(c -> c.getAccessor().isAnnotationPresent(PreAuthorize.class)
                                     || c.getAccessor().isAnnotationPresent(HasAuthority.class))
                        .findFirst()
                        .map(c -> evaluateSecurityAnnotations(c.getAccessor()))
                        .orElse(true);
            }
            return Arrays.stream(payloadType.getInterfaces())
                    .flatMap(iface -> Arrays.stream(iface.getMethods()))
                    .filter(m -> m.getName().equals(propertyName) && m.getParameterCount() == 0)
                    .filter(m -> m.isAnnotationPresent(PreAuthorize.class) || m.isAnnotationPresent(HasAuthority.class))
                    .findFirst()
                    .map(HalFormsInputPayloadMetadata::evaluateSecurityAnnotations)
                    .orElse(true);
        }

        private static Boolean evaluateSecurityAnnotations(Method m) {
            boolean result = true;
            if (m.isAnnotationPresent(PreAuthorize.class)) {
                result &= evaluatePreAuthorize(m);
            }
            if (m.isAnnotationPresent(HasAuthority.class)) {
                result &= evaluateHasAuthority(m);
            }
            return result;
        }

        private static boolean evaluateHasAuthority(Method method) {
            HasAuthority annotation = method.getAnnotation(HasAuthority.class);
            return SecuritySpelEvaluator.hasAuthority(
                    SecurityContextHolder.getContext().getAuthentication(),
                    annotation.value());
        }

        private static boolean evaluatePreAuthorize(Method method) {
            PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            org.springframework.security.core.Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();
            return SecuritySpelEvaluator.evaluate(annotation.value(), method, authentication);
        }

        private static Optional<AnnotatedElement> getAnnotatedElementForProperty(AffordanceModel.PayloadMetadata payloadMetadata, AffordanceModel.PropertyMetadata delegate) {
            String propertyName = delegate.getName();

            Class<?> payloadClass = payloadMetadata.getType();
            Assert.notNull(payloadClass, "payloadClass cannot be null");

            if (payloadClass.isRecord()) {
                RecordComponent[] components = payloadClass.getRecordComponents();
                return Stream.of(components)
                        .filter(c -> c.getName().equals(propertyName))
                        .map(AnnotatedElement.class::cast)
                        .findFirst();
            }

            try {
                return Optional.of(payloadClass.getDeclaredField(propertyName));
            } catch (NoSuchFieldException e) {
                LOG.debug("Didn't find field %s on class %s".formatted(propertyName, payloadClass.getName()));
            }

            return Optional.empty();
        }

        @Override
        public List<String> getI18nCodes() {
            return inputPayloadMetadata.getI18nCodes();
        }

        @Override
        public AffordanceModel.InputPayloadMetadata withMediaTypes(List<MediaType> mediaTypes) {
            return new HalFormsInputPayloadMetadata(inputPayloadMetadata.withMediaTypes(mediaTypes), optionsDef);
        }

        @Override
        public List<MediaType> getMediaTypes() {
            return inputPayloadMetadata.getMediaTypes();
        }

        @Override
        public Class<?> getType() {
            return inputPayloadMetadata.getType();
        }

        @Override
        public <T extends AffordanceModel.Named> T customize(T target, java.util.function.Function<AffordanceModel.PropertyMetadata, T> customizer) {
            return inputPayloadMetadata.customize(target, customizer);
        }
    }

    /**
     * PropertyMetadata wrapper that overrides isReadOnly() and provides correct type for DTO properties
     */
    private static class KlabisHalFormsPropertyMetadataWrapper implements AffordanceModel.PropertyMetadata {

        private final AffordanceModel.PropertyMetadata delegate;
        private final HalForms propertyAnnotation;
        private final boolean defaultIsReadOnly;
        private final boolean authorized;

        public KlabisHalFormsPropertyMetadataWrapper(AffordanceModel.PropertyMetadata delegate, AnnotatedElement propertyElement, boolean isRecord, boolean authorized) {
            this.delegate = delegate;
            this.defaultIsReadOnly = isRecord ? false : delegate.isReadOnly();
            this.propertyAnnotation = propertyElement.isAnnotationPresent(HalForms.class) ? propertyElement.getAnnotation(
                    HalForms.class) : null;
            this.authorized = authorized;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isRequired() {
            return delegate.isRequired();
        }

        @Override
        public boolean isReadOnly() {
            if (propertyAnnotation != null) {
                // Component has @HalForms annotation, use its access value
                HalForms.Access access = propertyAnnotation.access();

                return switch (access) {
                    case READ_ONLY -> true;
                    case NONE, READ_WRITE -> false;
                    case DEFAULT -> defaultIsReadOnly;
                };
            }

            return defaultIsReadOnly;
        }

        @Override
        public Optional<String> getPattern() {
            return delegate.getPattern();
        }

        @Override
        public org.springframework.core.ResolvableType getType() {
            return delegate.getType();
        }

        @Override
        public Number getMin() {
            return delegate.getMin();
        }

        @Override
        public Number getMax() {
            return delegate.getMax();
        }

        @Override
        public Long getMinLength() {
            return delegate.getMinLength();
        }

        @Override
        public Long getMaxLength() {
            return delegate.getMaxLength();
        }

        public boolean isDisplayed() {
            if (!authorized) {
                return false;
            }
            return propertyAnnotation == null || !HalForms.Access.NONE.equals(propertyAnnotation.access());
        }

        private Class<?> getEnclosedClass() {
            return delegate.getType().getRawClass();
        }

        @Override
        public String getInputType() {
            if (propertyAnnotation != null && StringUtils.hasLength(propertyAnnotation.formInputType())) {
                return propertyAnnotation.formInputType();
            }

            String result = delegate.getInputType();
            if (result == null) {
                result = getTypeFromClass(getEnclosedClass());
            }

            if (Optional.class.getSimpleName().equalsIgnoreCase(result)
                || JsonNullable.class.getSimpleName().equalsIgnoreCase(result)
                || isCollectionType()) {
                Class<?> generic0 = delegate.getType().getGeneric(0).getRawClass();
                if (isSupportedCollectionType(generic0)) {
                    // JsonNullable<Collection<T>> — unwrap both to get the element type
                    result = getTypeFromClass(delegate.getType().getGeneric(0).getGeneric(0).getRawClass());
                } else {
                    result = getTypeFromClass(generic0);
                }
            }

            return result;
        }

        boolean isCollectionType() {
            Class<?> enclosedClass = getEnclosedClass();
            if (isSupportedCollectionType(enclosedClass)) {
                return true;
            }
            // JsonNullable<Collection<T>> — treat as collection for multi=true HAL Forms rendering
            if (JsonNullable.class.isAssignableFrom(enclosedClass)) {
                Class<?> wrappedType = delegate.getType().getGeneric(0).getRawClass();
                return isSupportedCollectionType(wrappedType);
            }
            return false;
        }

        static boolean isSupportedCollectionType(Class<?> type) {
            return Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type);
        }

        private Optional<HtmlInputType> fromClass(Class<?> type) {
            return Optional.ofNullable(HtmlInputType.from(type));
        }

        private String getTypeFromClass(Class<?> type) {
            return fromClass(type).map(HtmlInputType::value).orElse(type.getSimpleName());
        }


    }

}

