package com.klabis.common.groups.infrastructure.restapi;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.exceptions.UserFacingErrorResponses;
import com.klabis.common.groups.domain.CannotRemoveLastOwnerException;
import com.klabis.common.groups.domain.DirectMemberAdditionNotAllowedException;
import com.klabis.common.groups.domain.MemberAlreadyInGroupException;
import com.klabis.common.groups.domain.MemberNameLookup;
import com.klabis.common.groups.domain.MemberNotInGroupException;
import com.klabis.common.groups.domain.OwnerCannotBeMemberException;
import com.klabis.common.mvc.MvcComponent;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Group exceptions only know the member's identifier; the user knows the person by name, so the name is
 * resolved here. A name that cannot be resolved is left out, never replaced by the identifier.
 */
@MvcComponent
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GroupsExceptionHandler {

    private final ObjectProvider<MemberNameLookup> memberNames;

    GroupsExceptionHandler(ObjectProvider<MemberNameLookup> memberNames) {
        this.memberNames = memberNames;
    }

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
        return namedMember(ex, ex.memberId(), HttpStatusCode.valueOf(422), "Cannot Remove Last Owner");
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
        return UserFacingErrorResponses.of(ex, HttpStatusCode.valueOf(422), "Direct Member Addition Not Allowed");
    }

    @ExceptionHandler(OwnerCannotBeMemberException.class)
    @ApiResponse(
            responseCode = "422",
            description = "Unprocessable entity - an owner cannot be a member of the same group",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleOwnerCannotBeMember(OwnerCannotBeMemberException ex) {
        return namedMember(ex, ex.memberId(), HttpStatusCode.valueOf(422), "Owner Cannot Be Member");
    }

    @ExceptionHandler(MemberAlreadyInGroupException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - the member is already in the group",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleMemberAlreadyInGroup(MemberAlreadyInGroupException ex) {
        return namedMember(ex, ex.memberId(), HttpStatus.BAD_REQUEST, "Bad Request");
    }

    @ExceptionHandler(MemberNotInGroupException.class)
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - the member is not in the group",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ErrorResponse handleMemberNotInGroup(MemberNotInGroupException ex) {
        return namedMember(ex, ex.memberId(), HttpStatus.BAD_REQUEST, "Bad Request");
    }

    private <E extends RuntimeException & UserFacingError> ErrorResponse namedMember(
            E ex, Object memberId, HttpStatusCode status, String title) {
        return UserFacingErrorResponses.of(ex, status, title, MemberNameParameter.of(memberNames.getIfAvailable(), memberId));
    }
}
