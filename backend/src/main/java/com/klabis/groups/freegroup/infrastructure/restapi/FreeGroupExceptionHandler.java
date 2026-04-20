package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.usergroup.InvitationNotCancellableException;
import com.klabis.common.usergroup.NotInvitedMemberException;
import com.klabis.groups.freegroup.domain.GroupOwnershipRequiredException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {FreeGroupController.class, PendingInvitationsController.class})
@Order(1)
class FreeGroupExceptionHandler {

    @ExceptionHandler(GroupOwnershipRequiredException.class)
    public ErrorResponse handleGroupOwnershipRequired(GroupOwnershipRequiredException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(403), ex.getMessage())
                .title("Group Ownership Required")
                .build();
    }

    @ExceptionHandler(InvitationNotCancellableException.class)
    public ErrorResponse handleInvitationNotCancellable(InvitationNotCancellableException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Invitation Cannot Be Cancelled")
                .build();
    }

    @ExceptionHandler(NotInvitedMemberException.class)
    public ErrorResponse handleNotInvitedMember(NotInvitedMemberException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(400), ex.getMessage())
                .title("Not Invited Member")
                .build();
    }
}
