package com.klabis.common.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.AffordanceModelFactory;
import org.springframework.hateoas.mediatype.ConfiguredAffordance;
import org.springframework.hateoas.server.core.DummyInvocationUtils;
import org.springframework.hateoas.server.core.LastInvocationAware;
import org.springframework.hateoas.server.core.MethodInvocation;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.afford;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

public class HalFormsSupport {

    private static LastInvocationAware getLastInvocationAware(Object invocation) {
        Assert.isInstanceOf(LastInvocationAware.class, invocation);

        return DummyInvocationUtils.getLastInvocationAware(invocation);
    }

    private static boolean isAuthorizedForInvocation(MethodInvocation invocation) {
//        AuthorizationManager<MethodInvocation> mgr = new PreAuthorizeAuthorizationManager();
//        AuthorizationDecision decision =
//                mgr.check(() -> SecurityContextHolder.getContext().getAuthentication(), invocation);
//        return decision != null && decision.isGranted();
        return true;
    }

    private static Optional<AffordanceModelFactory> getHalFormsModelFactory() {
        return SpringFactoriesLoader.loadFactories(AffordanceModelFactory.class, HalFormsSupport.class.getClassLoader())
                .stream().filter(f -> MediaTypes.HAL_FORMS_JSON.equals(f.getMediaType())).findFirst();
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
        Affordance modifiedResult = modifyAffordanceForHalForms(result, lastInvocationAware);

        return List.of(modifiedResult);
    }

    /**
     * Modifies affordance to apply @HalForms annotations from record components
     */
    private static Affordance modifyAffordanceForHalForms(Affordance affordance, LastInvocationAware invocation) {
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
                    return createModifiedAffordance(affordance);
                }
            }
        }

        return affordance;
    }

    /**
     * Creates new affordance using AffordanceModelFactory with modified InputPayloadMetadata
     */
    private static Affordance createModifiedAffordance(Affordance original) {
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
            if (model.getClass().getSimpleName().contains("HalForms")) {
                ConfiguredAffordance configured = new HalFormsConfiguredAffordance(model);
                AffordanceModel newModel = halFormsFactory.getAffordanceModel(configured);
                newModels.put(mediaType, newModel);
            } else {
                // For other media types, keep the original model
                newModels.put(mediaType, model);
            }
        }

        return new Affordance(newModels);
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

    public static WebMvcLinkBuilder linkToIfAuthorized(Object invocation) {
        return linkTo(invocation);
    }

    /**
     * ConfiguredAffordance wrapper that modifies InputPayloadMetadata based on @HalForms annotations
     */
    private static class HalFormsConfiguredAffordance implements ConfiguredAffordance {

        private final AffordanceModel delegate;
        private final HalFormsInputPayloadMetadata modifiedInput;

        public HalFormsConfiguredAffordance(AffordanceModel delegate) {
            this.delegate = delegate;
            this.modifiedInput = new HalFormsInputPayloadMetadata(delegate.getInput());
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
            return modifiedInput;
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
     * InputPayloadMetadata wrapper that modifies PropertyMetadata based on @HalForms annotations
     */
    private static class HalFormsInputPayloadMetadata implements AffordanceModel.InputPayloadMetadata {
        private static final Logger LOG = LoggerFactory.getLogger(KlabisHalFormsPropertyMetadataWrapper.class);

        private final AffordanceModel.InputPayloadMetadata inputPayloadMetadata;

        public HalFormsInputPayloadMetadata(AffordanceModel.InputPayloadMetadata inputPayloadMetadata) {
            this.inputPayloadMetadata = inputPayloadMetadata;
        }

        @Override
        public Stream<AffordanceModel.PropertyMetadata> stream() {
            // Modify property metadata stream based on @HalForms annotations
            return inputPayloadMetadata.stream()
                    .map(this::wrapPropertyMetadata)
                    .filter(this::isPropertyDisplayed);
        }

        private AffordanceModel.PropertyMetadata wrapPropertyMetadata(AffordanceModel.PropertyMetadata metadata) {
            boolean isPayloadClassRecord = inputPayloadMetadata.getType() != null && inputPayloadMetadata.getType()
                    .isRecord();

            return new KlabisHalFormsPropertyMetadataWrapper(metadata,
                    getAnnotatedElementForProperty(inputPayloadMetadata, metadata).orElseThrow(),
                    isPayloadClassRecord);
        }

        private boolean isPropertyDisplayed(AffordanceModel.PropertyMetadata propertyMetadata) {
            if (propertyMetadata instanceof KlabisHalFormsPropertyMetadataWrapper wrapper) {
                return wrapper.isDisplayed();
            }
            return true;
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
            return new HalFormsInputPayloadMetadata(inputPayloadMetadata.withMediaTypes(mediaTypes));
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

        public KlabisHalFormsPropertyMetadataWrapper(AffordanceModel.PropertyMetadata delegate, AnnotatedElement propertyElement, boolean isRecord) {
            this.delegate = delegate;
            this.defaultIsReadOnly = isRecord ? false : delegate.isReadOnly();
            this.propertyAnnotation = propertyElement.isAnnotationPresent(HalForms.class) ? propertyElement.getAnnotation(
                    HalForms.class) : null;
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
            return propertyAnnotation == null || !HalForms.Access.NONE.equals(propertyAnnotation.access());
        }

        private Class<?> getEncosedClass() {
            return delegate.getType().getRawClass();
        }

        @Override
        public String getInputType() {
            if (propertyAnnotation != null && StringUtils.hasLength(propertyAnnotation.formInputType())) {
                return propertyAnnotation.formInputType();
            }

            String result = delegate.getInputType();
            if (result == null) {
                result = getEncosedClass().getSimpleName();
            }
            return result;
        }
    }

}
