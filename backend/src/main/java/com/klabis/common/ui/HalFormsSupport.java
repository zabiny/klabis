package com.klabis.common.ui;

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
import org.springframework.web.bind.annotation.RequestBody;

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
                    return createModifiedAffordance(affordance, requestBodyType);
                }
            }
        }

        return affordance;
    }

    /**
     * Creates new affordance using AffordanceModelFactory with modified InputPayloadMetadata
     */
    private static Affordance createModifiedAffordance(Affordance original, Class<?> recordType) {
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
                ConfiguredAffordance configured = new HalFormsConfiguredAffordance(model, recordType);
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

        public HalFormsConfiguredAffordance(AffordanceModel delegate, Class<?> recordType) {
            this.delegate = delegate;
            this.modifiedInput = new HalFormsInputPayloadMetadata(delegate.getInput(), recordType);
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

        private final AffordanceModel.InputPayloadMetadata delegate;
        private final Class<?> recordType;

        public HalFormsInputPayloadMetadata(AffordanceModel.InputPayloadMetadata delegate, Class<?> recordType) {
            this.delegate = delegate;
            this.recordType = recordType;
        }

        @Override
        public Stream<AffordanceModel.PropertyMetadata> stream() {
            // Modify property metadata stream based on @HalForms annotations
            return delegate.stream().map(metadata -> {
                String propertyName = metadata.getName();

                // Find matching record component
                RecordComponent[] components = recordType.getRecordComponents();
                Optional<RecordComponent> component = Stream.of(components)
                        .filter(c -> c.getName().equals(propertyName))
                        .findFirst();

                // Determine the correct readOnly value
                boolean readOnly;

                if (component.isPresent() && component.get().isAnnotationPresent(HalForms.class)) {
                    // Component has @HalForms annotation, use its access value
                    HalForms.Access access = component.get().getAnnotation(HalForms.class).access();

                    readOnly = switch (access) {
                        case READ_ONLY -> true;
                        case WRITE_ONLY, NONE, READ_WRITE -> false;
                    };
                } else {
                    // No annotation - for records, default to false (writable) instead of true (read-only)
                    // Records are immutable in Java, but in HAL-FORMS we want them to be writable by default
                    readOnly = false;
                }

                // Always return wrapper with determined readOnly value
                return new HalFormsPropertyMetadataWrapper(metadata, readOnly);
            });
        }

        @Override
        public List<String> getI18nCodes() {
            return delegate.getI18nCodes();
        }

        @Override
        public AffordanceModel.InputPayloadMetadata withMediaTypes(List<MediaType> mediaTypes) {
            return new HalFormsInputPayloadMetadata(delegate.withMediaTypes(mediaTypes), recordType);
        }

        @Override
        public List<MediaType> getMediaTypes() {
            return delegate.getMediaTypes();
        }

        @Override
        public Class<?> getType() {
            return delegate.getType();
        }

        @Override
        public <T extends AffordanceModel.Named> T customize(T target, java.util.function.Function<AffordanceModel.PropertyMetadata, T> customizer) {
            return delegate.customize(target, customizer);
        }
    }

    /**
     * PropertyMetadata wrapper that overrides isReadOnly()
     */
    private static class HalFormsPropertyMetadataWrapper implements AffordanceModel.PropertyMetadata {

        private final AffordanceModel.PropertyMetadata delegate;
        private final boolean readOnlyOverride;

        public HalFormsPropertyMetadataWrapper(AffordanceModel.PropertyMetadata delegate, boolean readOnlyOverride) {
            this.delegate = delegate;
            this.readOnlyOverride = readOnlyOverride;
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
            return readOnlyOverride;
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

        @Override
        public String getInputType() {
            return delegate.getInputType();
        }
    }

}
