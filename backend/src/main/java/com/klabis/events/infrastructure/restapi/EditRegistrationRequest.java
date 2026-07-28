package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventCategoryId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EditRegistrationRequest(
        @NotBlank(message = "SI card number is required")
        @Pattern(regexp = "\\d{6,7}", message = "SI card number must be 6-7 digits")
        String siCardNumber,
        EventCategoryId categoryId
) {
}
