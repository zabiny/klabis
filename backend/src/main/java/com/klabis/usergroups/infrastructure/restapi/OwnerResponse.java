package com.klabis.usergroups.infrastructure.restapi;

import java.util.UUID;

record OwnerResponse(UUID id, String firstName, String lastName, String registrationNumber) {
}
