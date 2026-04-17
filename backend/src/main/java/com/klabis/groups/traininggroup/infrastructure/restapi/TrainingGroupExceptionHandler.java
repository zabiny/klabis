package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.traininggroup.application.MemberAlreadyInTrainingGroupException;
import com.klabis.groups.traininggroup.infrastructure.restapi.TrainingGroupController;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {TrainingGroupController.class})
@Order(1)
class TrainingGroupExceptionHandler {

    @ExceptionHandler(MemberAlreadyInTrainingGroupException.class)
    public ErrorResponse handleMemberAlreadyInTrainingGroup(MemberAlreadyInTrainingGroupException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Member Already In Training Group")
                .build();
    }
}
