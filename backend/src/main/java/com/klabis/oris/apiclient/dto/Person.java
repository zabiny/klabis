package com.klabis.oris.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Person(
        @JsonProperty("ID") String id,
        @JsonProperty("FirstName") String firstName,
        @JsonProperty("LastName") String lastName
) {}
