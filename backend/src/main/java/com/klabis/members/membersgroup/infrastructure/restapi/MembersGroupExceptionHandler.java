package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.common.usergroup.NotInvitedMemberException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {MembersGroupController.class, PendingInvitationsController.class})
@Order(1)
class MembersGroupExceptionHandler {

    @ExceptionHandler(NotInvitedMemberException.class)
    public ErrorResponse handleNotInvitedMember(NotInvitedMemberException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(400), ex.getMessage())
                .title("Not Invited Member")
                .build();
    }
}
