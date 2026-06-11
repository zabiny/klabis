package com.dpolach.api.orisclient.dto.lov;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LevelListEntry(
        @JsonProperty("ID") String id,
        @JsonProperty("Name") String name,
        @JsonProperty("Description_CZ") String descriptionCZ,
        @JsonProperty("Description_EN") String descriptionEN,
        @JsonProperty("NonOfficial") String nonOfficial
) {}
