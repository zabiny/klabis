package com.klabis.membershipfees.infrastructure.restapi;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record ChooseFeeChoiceRequest(
        @NotNull UUID membershipFeeGroupId
) {
}
