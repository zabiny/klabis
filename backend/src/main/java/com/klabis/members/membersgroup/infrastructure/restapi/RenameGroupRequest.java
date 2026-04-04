package com.klabis.members.membersgroup.infrastructure.restapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RenameGroupRequest(@NotBlank @Size(max = 200) String name) {
}
