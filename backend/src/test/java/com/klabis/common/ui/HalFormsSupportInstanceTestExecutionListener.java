package com.klabis.common.ui;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/**
 * Binds the static {@link HalFormsSupport} instance to the context of the running test and clears
 * it afterwards. Without this, the instance left behind by whichever cached context started last
 * leaks into tests running against another context and into plain unit tests, making their link
 * authorization depend on test execution order.
 */
public class HalFormsSupportInstanceTestExecutionListener implements TestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        HalFormsSupport instance = testContext.hasApplicationContext()
                ? testContext.getApplicationContext().getBeanProvider(HalFormsSupport.class).getIfAvailable()
                : null;
        HalFormsSupport.useInstance(instance);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        HalFormsSupport.useInstance(null);
    }
}
