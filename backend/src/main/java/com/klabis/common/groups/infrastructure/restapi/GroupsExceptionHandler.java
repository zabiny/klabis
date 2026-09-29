package com.klabis.common.groups.infrastructure.restapi;

import com.klabis.common.groups.domain.CannotRemoveLastOwnerException;
import com.klabis.common.groups.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.common.groups.domain.OwnerCannotBeRemovedFromGroupException;
import com.klabis.common.mvc.MvcComponent;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@MvcComponent
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
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
