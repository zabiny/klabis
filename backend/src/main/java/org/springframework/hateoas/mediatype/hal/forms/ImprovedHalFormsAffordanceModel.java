package org.springframework.hateoas.mediatype.hal.forms;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.QueryParameter;
import org.springframework.hateoas.mediatype.ConfiguredAffordance;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class ImprovedHalFormsAffordanceModel extends HalFormsAffordanceModel {

    public static ConfiguredAffordance fromModel(AffordanceModel model, List<String> propertiesOrder) {
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
                return new SortedInputPayloadMetadata(model.getInput(), propertiesOrder);
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
        })).map(this::updatePropertyMetadata);
    }

    private AffordanceModel.PropertyMetadata updatePropertyMetadata(AffordanceModel.PropertyMetadata item) {
        // TODO: optimize this - instead of doing it on-the-fly during return, do it when initializing this class instead
        if (Record.class.isAssignableFrom(delegate.getType())) {
            ObjectMapper objectMapper = new ObjectMapper();
            JavaType javaType = objectMapper.getTypeFactory().constructType(delegate.getType());
            BeanDescription beanDescription = objectMapper.getSerializationConfig().introspect(javaType);

            item = new RecordPropertyMetadata(item, beanDescription);
        }
        return item;
    }

    static class RecordPropertyMetadata implements AffordanceModel.PropertyMetadata {

        private final AffordanceModel.PropertyMetadata delegate;
        private final BeanDescription beanDescription;

        RecordPropertyMetadata(AffordanceModel.PropertyMetadata delegate, BeanDescription beanDescription) {
            this.delegate = delegate;
            this.beanDescription = beanDescription;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public boolean isRequired() {
            return delegate.isRequired();
        }

        private BeanPropertyDefinition getPropertyDefinition() {
            return beanDescription.findProperties()
                    .stream()
                    .filter(adept -> StringUtils.equals(delegate.getName(), adept.getName()))
                    .findFirst()
                    .orElseThrow();
        }

        @Override
        public boolean isReadOnly() {
            BeanPropertyDefinition propertyDefinition = getPropertyDefinition();
            if (propertyDefinition.couldDeserialize()) {
                JsonProperty propertyAnnotation = propertyDefinition.getField().getAnnotation(JsonProperty.class);
                if (propertyAnnotation != null) {
                    return JsonProperty.Access.READ_ONLY.equals(propertyAnnotation.access());
                } else {
                    return false;
                }
            } else {
                return true;
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
            String result = delegate.getInputType();

            if (StringUtils.isBlank(result)) {
                if (getType().getRawClass() != null) {
                    if (Enum.class.isAssignableFrom(getType().getRawClass())) {
                        return "radio";
                    }

                    return getType().getType().getTypeName();
                }
            }

            return result;
        }
    }

}