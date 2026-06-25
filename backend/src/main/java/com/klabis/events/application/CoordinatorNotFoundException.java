package com.klabis.events.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.members.MemberId;

import java.util.Collection;
import java.util.stream.Collectors;

public class CoordinatorNotFoundException extends ResourceNotFoundException {

    public CoordinatorNotFoundException(MemberId memberId) {
        super("Coordinator member not found: " + memberId.value());
    }

    public CoordinatorNotFoundException(Collection<MemberId> memberIds) {
        super("Coordinator members not found: " + memberIds.stream()
                .map(id -> id.value().toString())
                .collect(Collectors.joining(", ")));
    }
}
