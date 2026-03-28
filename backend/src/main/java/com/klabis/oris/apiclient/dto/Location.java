package com.klabis.oris.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Location(
        @JsonProperty("ID") String id,
        @JsonProperty("Name") String name,
        @JsonProperty("Description") String description
) {}
