package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.members.familygroup.application.MemberAlreadyInFamilyGroupException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FamilyGroupController.class)
@Order(1)
class FamilyGroupExceptionHandler {

    @ExceptionHandler(MemberAlreadyInFamilyGroupException.class)
    public ErrorResponse handleMemberAlreadyInFamilyGroup(MemberAlreadyInFamilyGroupException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(400), ex.getMessage())
                .title("Member Already In Family Group")
                .build();
    }

    @ExceptionHandler(CannotRemoveLastOwnerException.class)
    public ErrorResponse handleCannotRemoveLastParent(CannotRemoveLastOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Cannot Remove Last Parent")
                .build();
    }
}
