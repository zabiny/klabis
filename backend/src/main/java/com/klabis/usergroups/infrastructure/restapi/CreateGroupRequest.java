package com.klabis.usergroups.infrastructure.restapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateGroupRequest(@NotBlank @Size(max = 200) String name) {
}
