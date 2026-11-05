package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.exceptions.UserFacingErrorResponses;
import com.klabis.common.groups.domain.MemberNameLookup;
import com.klabis.common.groups.infrastructure.restapi.MemberNameParameter;
import com.klabis.groups.freegroup.domain.CannotInviteExistingMemberException;
import com.klabis.groups.freegroup.domain.CannotPromoteNonMemberToOwnerException;
import com.klabis.groups.freegroup.domain.DuplicatePendingInvitationException;
import com.klabis.groups.freegroup.domain.GroupOwnershipRequiredException;
import com.klabis.groups.freegroup.domain.NotInvitedMemberException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {FreeGroupController.class, PendingInvitationsController.class})
@Order(1)
class FreeGroupExceptionHandler {

    private final ObjectProvider<MemberNameLookup> memberNames;

    FreeGroupExceptionHandler(ObjectProvider<MemberNameLookup> memberNames) {
        this.memberNames = memberNames;
    }

    @ExceptionHandler(GroupOwnershipRequiredException.class)
    public ErrorResponse handleGroupOwnershipRequired(GroupOwnershipRequiredException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.FORBIDDEN, "Group Ownership Required");
    }

    @ExceptionHandler(NotInvitedMemberException.class)
    public ErrorResponse handleNotInvitedMember(NotInvitedMemberException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.BAD_REQUEST, "Not Invited Member");
    }

    @ExceptionHandler(CannotInviteExistingMemberException.class)
    public ErrorResponse handleCannotInviteExistingMember(CannotInviteExistingMemberException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.BAD_REQUEST, "Bad Request",
                MemberNameParameter.of(memberNames.getIfAvailable(), ex.getMemberId()));
    }

    @ExceptionHandler(DuplicatePendingInvitationException.class)
    public ErrorResponse handleDuplicatePendingInvitation(DuplicatePendingInvitationException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.BAD_REQUEST, "Bad Request",
                MemberNameParameter.of(memberNames.getIfAvailable(), ex.getMemberId()));
    }

    @ExceptionHandler(CannotPromoteNonMemberToOwnerException.class)
    public ErrorResponse handleCannotPromoteNonMemberToOwner(CannotPromoteNonMemberToOwnerException ex) {
        return UserFacingErrorResponses.of(ex, HttpStatus.CONFLICT, "Conflict",
                MemberNameParameter.of(memberNames.getIfAvailable(), ex.getMemberId()));
    }
}
