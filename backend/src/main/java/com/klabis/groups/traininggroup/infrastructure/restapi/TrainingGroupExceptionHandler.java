package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.exceptions.UserFacingErrorResponses;
import com.klabis.common.groups.domain.MemberNameLookup;
import com.klabis.common.groups.infrastructure.restapi.MemberNameParameter;
import com.klabis.groups.traininggroup.application.MemberAlreadyInTrainingGroupException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TrainingGroupController.class)
@Order(1)
class TrainingGroupExceptionHandler {

    private final ObjectProvider<MemberNameLookup> memberNames;

    TrainingGroupExceptionHandler(ObjectProvider<MemberNameLookup> memberNames) {
        this.memberNames = memberNames;
    }

    @ExceptionHandler(MemberAlreadyInTrainingGroupException.class)
    public ErrorResponse handleMemberAlreadyInTrainingGroup(MemberAlreadyInTrainingGroupException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.CONFLICT, "Conflict",
                MemberNameParameter.of(memberNames.getIfAvailable(), ex.getMemberId()));
    }
}
