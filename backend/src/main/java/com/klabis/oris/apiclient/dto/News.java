package com.klabis.oris.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record News(
        @JsonProperty("ID") String id,
        @JsonProperty("Title") String title,
        @JsonProperty("Content") String content,
        @JsonProperty("Date") String date
) {}
