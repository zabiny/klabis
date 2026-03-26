package com.klabis.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base test class for security-related tests.
 * <p>
 * Provides common setup for testing secured endpoints.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class SecurityTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_ROLE = "ADMIN";

    protected static final String MEMBER_USERNAME = "ZBM0101";
}
