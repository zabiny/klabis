package club.klabis.shared.config.ddd.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collection;

@Configuration
class FormHandlerConfiguration implements SmartInitializingSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(FormHandlerConfiguration.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final ApplicationContext context;


    public FormHandlerConfiguration(RequestMappingHandlerMapping requestMappingHandlerMapping, ApplicationContext context) {
        this.handlerMapping = requestMappingHandlerMapping;
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        registerFormHandlerEndpoints();
    }

    private void registerFormHandlerEndpoints() {
        FormHandlersRegistry formHandlersRegistry = context.getBean(FormHandlersRegistry.class);
        Collection<FormApiDescriptor<?>> formApiDescriptors = formHandlersRegistry.getFormApis();

        LOG.info("Found {} form handlers", formApiDescriptors.size());

        for (FormApiDescriptor<?> formApiDescriptor : formApiDescriptors) {
            LOG.info("Registering controllers for form %s with handler %s".formatted(formApiDescriptor.formType(),
                    formApiDescriptor.formHandler()));

            // Register GET endpoint
            try {
                RequestMappingInfo info = RequestMappingInfo.paths(formApiDescriptor.apiPath())
                        .methods(RequestMethod.GET)
                        .build();
                handlerMapping.registerMapping(
                        info,
                        new FormHandlerController<>(formApiDescriptor.formHandler()),
                        FormHandlerController.class.getMethod("getFormData")
                );

                LOG.info("- GET %s endpoint registered".formatted(info.getDirectPaths()));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to register GET endpoint for form handler of type %s".formatted(
                        formApiDescriptor.getClass()));
            }

            // Register PUT endpoint
            try {
                RequestMappingInfo info = RequestMappingInfo.paths(formApiDescriptor.apiPath())
                        .methods(RequestMethod.PUT)
                        .build();
                handlerMapping.registerMapping(
                        info,
                        new FormHandlerController<>(formApiDescriptor.formHandler()),
                        FormHandlerController.class.getMethod("submitFormData", Object.class)
                );

                LOG.info("- PUT %s endpoint registered".formatted(info.getDirectPaths()));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to register PUT endpoint for form handler of type %s".formatted(
                        formApiDescriptor.getClass()), e);
            }
        }
    }
}
