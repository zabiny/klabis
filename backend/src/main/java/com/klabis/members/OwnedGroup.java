package com.klabis.members;

import java.net.URI;

public record OwnedGroup(String groupId, String groupName, String groupType, URI groupLink) {}
