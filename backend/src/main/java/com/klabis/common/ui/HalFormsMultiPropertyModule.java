package com.klabis.common.ui;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.hateoas.AffordanceModel;
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

            return beanProperties.stream()
                    .map(writer -> "multi".equals(writer.getName()) ? new MultiPropertyWriter(writer) : writer)
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

        private static String getHalFormsPropertyName(Object bean) {
            if (bean instanceof AffordanceModel.Named named) {
                return named.getName();
            }
            return null;
        }
    }
}
