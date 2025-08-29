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
    private final FormHandlerControllersFactory formHandlerControllersFactory;

    public FormHandlerConfiguration(RequestMappingHandlerMapping requestMappingHandlerMapping, ApplicationContext context, FormHandlerControllersFactory formHandlerControllersFactory) {
        this.handlerMapping = requestMappingHandlerMapping;
        this.context = context;
        this.formHandlerControllersFactory = formHandlerControllersFactory;
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

            var controller = formHandlerControllersFactory.createController(formApiDescriptor);

            // Register GET endpoints - return form data and return form schema
            try {
                RequestMappingInfo info = RequestMappingInfo.paths(formApiDescriptor.apiPath())
                        .methods(RequestMethod.GET)
                        .produces("application/json")
                        .build();
                handlerMapping.registerMapping(
                        info,
                        controller,
                        FormHandlerController.class.getMethod("getFormData")
                );

                info = RequestMappingInfo.paths(formApiDescriptor.apiPath())
                        .methods(RequestMethod.GET)
                        .produces("application/schema+json")
                        .build();
                handlerMapping.registerMapping(
                        info,
                        controller,
                        FormHandlerController.class.getMethod("getFormDataSchema")
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
                        .consumes("application/json")
                        .build();
                handlerMapping.registerMapping(
                        info,
                        controller,
                        FormHandlerController.class.getMethod("submitFormData", String.class)
                );

                LOG.info("- PUT %s endpoint registered".formatted(info.getDirectPaths()));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Failed to register PUT endpoint for form handler of type %s".formatted(
                        formApiDescriptor.getClass()), e);
            }
        }
    }
}
