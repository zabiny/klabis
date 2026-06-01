package com.klabis.common.ui;

/**
 * A value+prompt pair for HAL-FORMS inline options.
 * Jackson serializes this as {"value": "...", "prompt": "..."} so HAL-FORMS clients
 * can display human-readable labels while submitting the raw value.
 */
public record HalFormsInlineOption(String value, String prompt) {
}
