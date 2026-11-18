package com.klabis.common.ui;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.util.List;

@JacksonComponent
class HalFormsMultiPropertyModule extends SimpleModule {

    HalFormsMultiPropertyModule() {
        super("HalFormsMultiPropertyModule");
        setSerializerModifier(new HalFormsPropertySerializerModifier());
    }

    private static class HalFormsPropertySerializerModifier extends ValueSerializerModifier {

        private static final String HAL_FORMS_PROPERTY_CLASS = "org.springframework.hateoas.mediatype.hal.forms.HalFormsProperty";

        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                         BeanDescription.Supplier beanDescSupplier,
                                                         List<BeanPropertyWriter> beanProperties) {
            if (!HAL_FORMS_PROPERTY_CLASS.equals(beanDescSupplier.get().getBeanClass().getName())) {
                return beanProperties;
            }

            BeanPropertyWriter typeWriter = beanProperties.stream()
                    .filter(writer -> "type".equals(writer.getName()))
                    .findFirst()
                    .orElseThrow();
            return beanProperties.stream()
                    .map(writer -> switch (writer.getName()) {
                        case "multi" -> new MultiPropertyWriter(writer);
                        case "options" -> new OptionsPropertyWriter(writer, typeWriter);
                        case "type" -> new TypePropertyWriter(writer);
                        default -> writer;
                    })
                    .toList();
        }
    }

    private static class MultiPropertyWriter extends BeanPropertyWriter {

        MultiPropertyWriter(BeanPropertyWriter delegate) {
            super(delegate);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
            String propertyName = getHalFormsPropertyName(bean);
            if (propertyName != null && CollectionPropertyContext.isCollectionProperty(propertyName)) {
                gen.writeName("multi");
                gen.writeBoolean(true);
            } else {
                super.serializeAsProperty(bean, gen, prov);
            }
        }
    }

    private static class TypePropertyWriter extends BeanPropertyWriter {

        TypePropertyWriter(BeanPropertyWriter delegate) {
            super(delegate);
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
            Object type = get(bean);
            if (type instanceof String text && text.indexOf(HalFormsSupport.OPTIONS_TOKEN_SEPARATOR) >= 0) {
                String plain = HalFormsSupport.stripOptionsToken(text);
                if (!plain.isEmpty()) {
                    gen.writeStringProperty("type", plain);
                }
                return;
            }
            super.serializeAsProperty(bean, gen, prov);
        }
    }

    private static class OptionsPropertyWriter extends BeanPropertyWriter {

        private final BeanPropertyWriter typeWriter;

        OptionsPropertyWriter(BeanPropertyWriter delegate, BeanPropertyWriter typeWriter) {
            super(delegate);
            this.typeWriter = typeWriter;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
            if (typeWriter.get(bean) instanceof String type) {
                HalFormsOptionsDef optionsDef = HalFormsSupport.getOptionsDefForType(type);
                HalFormsOptions options = switch (optionsDef) {
                    case HalFormsOptionsDef.Values values when !values.values().isEmpty() ->
                            HalFormsOptions.inline(values.values().toArray());
                    case HalFormsOptionsDef.Inline inline when !inline.values().isEmpty() ->
                            HalFormsOptions.inline(inline.values()).withValueField("value").withPromptField("prompt");
                    case HalFormsOptionsDef.Remote remote -> HalFormsOptions.remote(remote.link());
                    case null, default -> null;
                };

                if (options != null) {
                    gen.writeName("options");
                    prov.findValueSerializer(options.getClass()).serialize(options, gen, prov);
                    return;
                }

                // No explicit (request-scoped) options: fall through to the bean's own "options"
                // field, already populated for auto-detected enums by EnumOptionsAutoConfiguration.
            }

            super.serializeAsProperty(bean, gen, prov);
        }
    }

    private static String getHalFormsPropertyName(Object bean) {
        if (bean instanceof AffordanceModel.Named named) {
            return named.getName();
        }
        return null;
    }
}
