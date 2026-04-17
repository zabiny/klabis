package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.groups.familygroup.application.MemberAlreadyInFamilyGroupException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {FamilyGroupController.class})
@Order(1)
class FamilyGroupExceptionHandler {

    @ExceptionHandler(MemberAlreadyInFamilyGroupException.class)
    public ErrorResponse handleMemberAlreadyInFamilyGroup(MemberAlreadyInFamilyGroupException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Member Already In Family Group")
                .build();
    }
}
