package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.groups.freegroup.domain.CannotPromoteNonMemberToOwnerException;
import com.klabis.groups.freegroup.domain.InvitationNotCancellableException;
import com.klabis.groups.freegroup.domain.NotInvitedMemberException;
import com.klabis.groups.freegroup.domain.GroupOwnershipRequiredException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(CannotPromoteNonMemberToOwnerException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - cannot promote a non-member to owner",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleCannotPromoteNonMemberToOwner(CannotPromoteNonMemberToOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage())
                .title("Cannot Promote Non-Member to Owner")
                .build();
    }
}
