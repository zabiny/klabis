package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.usergroups.domain.UserGroup;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(1)
class GroupsExceptionHandler {

    @ExceptionHandler(DirectMemberAdditionNotAllowedException.class)
    public ErrorResponse handleDirectMemberAdditionNotAllowed(DirectMemberAdditionNotAllowedException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Direct Member Addition Not Allowed")
                .build();
    }

    @ExceptionHandler(UserGroup.CannotRemoveLastOwnerException.class)
    public ErrorResponse handleCannotRemoveLastOwner(UserGroup.CannotRemoveLastOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Cannot Remove Last Owner")
                .build();
    }

}
