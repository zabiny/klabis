package com.klabis.groups.common.infrastructure.restapi;

import com.klabis.groups.common.domain.CannotPromoteNonMemberToOwnerException;
import com.klabis.groups.common.domain.CannotRemoveLastOwnerException;
import com.klabis.groups.common.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.groups.common.domain.OwnerCannotBeRemovedFromGroupException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GroupsExceptionHandler {

    @ExceptionHandler(CannotRemoveLastOwnerException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - cannot remove the last owner of a group",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleCannotRemoveLastOwner(CannotRemoveLastOwnerException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Cannot Remove Last Owner")
                .build();
    }

    @ExceptionHandler(DirectMemberAdditionNotAllowedException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - direct member addition not allowed for invitation-based groups",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleDirectMemberAdditionNotAllowed(DirectMemberAdditionNotAllowedException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Direct Member Addition Not Allowed")
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

    @ExceptionHandler(OwnerCannotBeRemovedFromGroupException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - owner cannot be removed from a group directly",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleOwnerCannotBeRemovedFromGroup(OwnerCannotBeRemovedFromGroupException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(422), ex.getMessage())
                .title("Owner Cannot Be Removed From Group")
                .build();
    }
}
