package com.klabis.common.ui;

import com.klabis.common.ui.infrastructure.restapi.AutoEnumOptionsExampleController;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Example controllers of the {@code common.ui} module tests, imported as one set so those tests
 * end up with an identical context configuration and share a single cached Spring context.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({
        AutoEnumOptionsExampleController.class,
        HalFormsExampleController.class,
        ExamplePostprocessor.class,
        OptionsDefExampleController.class
})
class CommonUiExampleControllers {
}
