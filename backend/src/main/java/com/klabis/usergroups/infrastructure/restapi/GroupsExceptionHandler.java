package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.domain.DirectMemberAdditionNotAllowedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GroupsExceptionHandler {

    @ExceptionHandler(DirectMemberAdditionNotAllowedException.class)
    public ErrorResponse handleDirectMemberAdditionNotAllowed(DirectMemberAdditionNotAllowedException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Direct Member Addition Not Allowed")
                .build();
    }
}
