package com.klabis.usergroups;

import java.util.UUID;

public record UserGroupOwnershipInfo(UUID groupId, String groupName, String groupType) {
}
