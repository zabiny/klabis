package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.MemberIsLastGroupOwnerException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
class MembersExceptionHandler {

    record LastOwnerWarning(String message, List<AffectedGroup> affectedGroups) {
        record AffectedGroup(String groupId, String groupName, String groupType) {
        }
    }

    @ExceptionHandler(MemberIsLastGroupOwnerException.class)
    ResponseEntity<LastOwnerWarning> handleMemberIsLastGroupOwner(MemberIsLastGroupOwnerException ex) {
        List<LastOwnerWarning.AffectedGroup> affectedGroups = ex.getGroups().stream()
                .map(info -> new LastOwnerWarning.AffectedGroup(
                        info.groupId(),
                        info.groupName(),
                        info.groupType()))
                .toList();

        return ResponseEntity
                .status(HttpStatusCode.valueOf(409))
                .body(new LastOwnerWarning(ex.getMessage(), affectedGroups));
    }
}
