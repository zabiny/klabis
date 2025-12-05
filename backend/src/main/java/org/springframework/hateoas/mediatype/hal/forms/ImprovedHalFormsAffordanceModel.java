package org.springframework.hateoas.mediatype.hal.forms;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.QueryParameter;
import org.springframework.hateoas.mediatype.ConfiguredAffordance;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class ImprovedHalFormsAffordanceModel extends HalFormsAffordanceModel {

    private static final Logger LOG = LoggerFactory.getLogger(ImprovedHalFormsAffordanceModel.class);

    private final HalFormsPropertyCompositePostprocessor halFormsPropertyPostprocessor = HalFormsPropertyCompositePostprocessor.defaultSetup();
    private static final Jackson3Adapter jacksonAdapter = new Jackson3Adapter();

    public static ImprovedHalFormsAffordanceModel improveHalFormsAffordance(AffordanceModel original) {

        return new ImprovedHalFormsAffordanceModel(ImprovedHalFormsAffordanceModel.fromModel(original,
                getPropertiesOrder(original)));
    }

    static List<String> getPropertiesOrder(AffordanceModel model) {
        if (model.getInput().getType() != null) {
            // for example API endpoint doesn't have any request body...
            Class<?> requestBodyType = model.getInput().getType();

            return expectedPropertiesOrder(requestBodyType);
        } else {
            return List.of();
        }
    }

    public void defineOptions(String propertyName, HalFormsOptions options) {
        this.halFormsPropertyPostprocessor.findPostprocessor(HalFormsPropertyOptionsPostprocessor.class)
                .ifPresentOrElse(p -> p.defineOptions(propertyName, options),
                        () -> LOG.warn("No InputOptions postprocessor found in active postprocessors"));
    }

    static List<String> expectedPropertiesOrder(Class<?> bodyType) {
        return jacksonAdapter.streamJacksonPropertyDefinitions(bodyType)
                .map(BeanPropertyDefinition::getName)
                .toList();
    }

    static ConfiguredAffordance fromModel(AffordanceModel model, List<String> propertiesOrder) {

        return new ConfiguredAffordance() {
            @Override
            public String getNameOrDefault() {
                return model.getName();
            }

            @Override
            public Link getTarget() {
                return model.getLink();
            }

            @Override
            public HttpMethod getMethod() {
                return model.getHttpMethod();
            }

            @Override
            public InputPayloadMetadata getInputMetadata() {
                InputPayloadMetadata metadata = new SortedInputPayloadMetadata(model.getInput(), propertiesOrder);
                return metadata;
            }

            @Override
            public List<QueryParameter> getQueryParameters() {
                return model.getQueryMethodParameters();
            }

            @Override
            public PayloadMetadata getOutputMetadata() {
                return model.getOutput();
            }
        };
    }

    public ImprovedHalFormsAffordanceModel(ConfiguredAffordance configured) {
        super(configured);
    }

    // Method used in HalFormsPropertiesFactory to create properties. We want improve some property data, so let's do that :)
    @Override
    public <T> List<T> createProperties(BiFunction<InputPayloadMetadata, PropertyMetadata, T> creator) {
        CreatorWithPostprocess<T> creatorWithPostprocess = new CreatorWithPostprocess<>(creator,
                this.halFormsPropertyPostprocessor);
        return super.createProperties(creatorWithPostprocess);
    }

    private static class CreatorWithPostprocess<T> implements BiFunction<InputPayloadMetadata, PropertyMetadata, T> {

        private final BiFunction<InputPayloadMetadata, PropertyMetadata, T> decoratedCreator;
        private final HalFormsPropertyPostprocessor halFormsPropertyPostprocessor;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private CreatorWithPostprocess(BiFunction<InputPayloadMetadata, PropertyMetadata, T> decoratedCreator, HalFormsPropertyPostprocessor halFormsPropertyPostprocessor) {
            this.decoratedCreator = decoratedCreator;
            this.halFormsPropertyPostprocessor = halFormsPropertyPostprocessor;
        }

        @Override
        public T apply(InputPayloadMetadata inputPayloadMetadata, PropertyMetadata propertyMetadata) {
            T result = decoratedCreator.apply(inputPayloadMetadata,
                    improvePropertyMetadata(inputPayloadMetadata, propertyMetadata));

            if (result instanceof HalFormsProperty typedProp) {
                result = (T) halFormsPropertyPostprocessor.postprocess(typedProp, propertyMetadata);
            }

            return result;
        }

        private AffordanceModel.PropertyMetadata improvePropertyMetadata(InputPayloadMetadata payload, final AffordanceModel.PropertyMetadata property) {
            BeanPropertyDefinition propertyDefinition = jacksonAdapter.getBeanPropertyDefinition(
                            payload.getType(),
                            property.getName())
                    .orElseThrow();

            return new ImprovedPropertyMetadata(payload, property, propertyDefinition);
        }

    }

}

class Jackson3Adapter {

    Jackson3Adapter() {
        this(JsonMapper.builder()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
                .build());
    }

    Jackson3Adapter(JsonMapper jsonMapper) {
        JSON_MAPPER = jsonMapper;
        JACKSON_CLASS_INTROSPECTOR = jsonMapper.serializationConfig().classIntrospectorInstance();
    }

    private final JsonMapper JSON_MAPPER;

    private final ClassIntrospector JACKSON_CLASS_INTROSPECTOR;

    public Stream<BeanPropertyDefinition> streamJacksonPropertyDefinitions(Class<?> type) {
        return getBeanDescription(type).findProperties().stream();
    }

    public Optional<BeanPropertyDefinition> getBeanPropertyDefinition(Class<?> type, String propertyName) {
        return streamJacksonPropertyDefinitions(type)
                .filter(adept -> StringUtils.equals(propertyName, adept.getName()))
                .findFirst();
    }

    private BeanDescription getBeanDescription(Class<?> type) {
        JavaType javaType = JSON_MAPPER.getTypeFactory().constructType(type);
        AnnotatedClass annotatedJavaType = JACKSON_CLASS_INTROSPECTOR.introspectClassAnnotations(javaType);

        BeanDescription beanDescription = JACKSON_CLASS_INTROSPECTOR.introspectForSerialization(
                javaType,
                annotatedJavaType);

        return beanDescription;

    }
}

/**
 * Sorts attribute metadata into defined order (so displaying form from that metadata have defined order of attributes)
 */
class SortedInputPayloadMetadata implements AffordanceModel.InputPayloadMetadata {
    private final AffordanceModel.InputPayloadMetadata delegate;
    private final List<String> propertiesOrder;

    SortedInputPayloadMetadata(AffordanceModel.InputPayloadMetadata delegate, List<String> propertiesOrder) {
        this.delegate = delegate;
        this.propertiesOrder = propertiesOrder;
    }

    @Override
    public Class<?> getType() {
        return delegate.getType();
    }

    @Override
    public <T extends AffordanceModel.Named> T customize(T target, Function<AffordanceModel.PropertyMetadata, T> customizer) {
        return delegate.customize(target, customizer);
    }

    @Override
    public List<String> getI18nCodes() {
        return delegate.getI18nCodes();
    }

    @Override
    public AffordanceModel.InputPayloadMetadata withMediaTypes(List<MediaType> mediaType) {
        return delegate.withMediaTypes(mediaType);
    }

    @Override
    public List<MediaType> getMediaTypes() {
        return delegate.getMediaTypes();
    }

    @Override
    public Stream<AffordanceModel.PropertyMetadata> stream() {
        return delegate.stream().sorted(Comparator.comparingInt(p -> {
            int idx = propertiesOrder.indexOf(p.getName());
            return idx >= 0 ? idx : Integer.MAX_VALUE;
        }));
    }

}

class ImprovedPropertyMetadata implements AffordanceModel.PropertyMetadata {

    private final AffordanceModel.PropertyMetadata delegate;
    private final BeanPropertyDefinition beanDescription;
    private final AffordanceModel.PayloadMetadata payloadMetadata;

    ImprovedPropertyMetadata(AffordanceModel.PayloadMetadata payloadMetadata, AffordanceModel.PropertyMetadata delegate, BeanPropertyDefinition beanDescription) {
        this.delegate = delegate;
        this.beanDescription = beanDescription;
        this.payloadMetadata = payloadMetadata;
    }

    private boolean isRecordPayload() {
        return payloadMetadata.getType() != null && Record.class.isAssignableFrom(payloadMetadata.getType());
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isRequired() {
        return delegate.isRequired();
    }

    private static Annotated getAnnotatedElement(BeanPropertyDefinition propertyDefinition) {
        return Stream.of(propertyDefinition.getConstructorParameter(), propertyDefinition.getField())
                .filter(Objects::nonNull)
                .findFirst().orElseThrow();
    }

    @Override
    public boolean isReadOnly() {
        if (isRecordPayload()) {
            if (beanDescription.couldDeserialize()) {
                JsonProperty propertyAnnotation = getAnnotatedElement(beanDescription).getAnnotation(JsonProperty.class);
                if (propertyAnnotation != null) {
                    return JsonProperty.Access.READ_ONLY.equals(propertyAnnotation.access());
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return delegate.isReadOnly();
        }
    }

    @Override
    public Optional<String> getPattern() {
        return delegate.getPattern();
    }

    @Override
    public ResolvableType getType() {
        return delegate.getType();
    }

    @Override
    public String getInputType() {
        return delegate.getInputType();
    }
}
