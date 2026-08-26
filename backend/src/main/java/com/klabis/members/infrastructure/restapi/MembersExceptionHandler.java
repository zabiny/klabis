package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.SuspensionBlockedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
class MembersExceptionHandler {

    private final MemberMapper memberMapper;

    MembersExceptionHandler(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @ExceptionHandler(SuspensionBlockedException.class)
    ResponseEntity<SuspensionBlockedWarning> handleSuspensionBlocked(SuspensionBlockedException ex) {
        LastOwnerWarning groups = null;
        if (!ex.getBlockingGroups().isEmpty()) {
            List<AffectedGroup> affectedGroups = ex.getBlockingGroups().stream()
                    .map(info -> new AffectedGroup(
                            UUID.fromString(info.groupId()),
                            info.groupName(),
                            AffectedGroupGroupType.fromValue(info.groupType())))
                    .toList();
            groups = new LastOwnerWarning(
                    affectedGroups,
                    "Member is the last owner of %d group(s) — designate a successor before suspension"
                            .formatted(affectedGroups.size()));
        }

        OutstandingDebtWarning debt = null;
        if (ex.getDebtSnapshot() != null) {
            var snapshot = ex.getDebtSnapshot();
            URI accountLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/members/{memberId}/account")
                    .buildAndExpand(snapshot.memberId().uuid())
                    .toUri();
            debt = new OutstandingDebtWarning(accountLink, memberMapper.monetaryAmountToDto(snapshot.balance()));
        }

        return ResponseEntity
                .status(HttpStatusCode.valueOf(409))
                .body(new SuspensionBlockedWarning(debt, groups));
    }
}
