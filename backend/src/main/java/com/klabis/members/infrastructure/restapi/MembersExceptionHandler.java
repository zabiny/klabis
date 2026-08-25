package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MonetaryAmount;
import com.klabis.members.application.SuspensionBlockedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestControllerAdvice
class MembersExceptionHandler {

    record LastOwnerWarning(String message, List<AffectedGroup> affectedGroups) {
        record AffectedGroup(String groupId, String groupName, String groupType) {
        }
    }

    record OutstandingDebtWarning(MonetaryAmount balance, String accountLink) {
    }

    @ExceptionHandler(SuspensionBlockedException.class)
    ResponseEntity<SuspensionBlockedWarning> handleSuspensionBlocked(SuspensionBlockedException ex) {
        LastOwnerWarning groups = null;
        if (!ex.getBlockingGroups().isEmpty()) {
            List<LastOwnerWarning.AffectedGroup> affectedGroups = ex.getBlockingGroups().stream()
                    .map(info -> new LastOwnerWarning.AffectedGroup(
                            info.groupId(),
                            info.groupName(),
                            info.groupType()))
                    .toList();
            groups = new LastOwnerWarning(
                    "Member is the last owner of %d group(s) — designate a successor before suspension"
                            .formatted(affectedGroups.size()),
                    affectedGroups);
        }

        OutstandingDebtWarning debt = null;
        if (ex.getDebtSnapshot() != null) {
            var snapshot = ex.getDebtSnapshot();
            String accountLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/members/{memberId}/account")
                    .buildAndExpand(snapshot.memberId().uuid())
                    .toUriString();
            debt = new OutstandingDebtWarning(snapshot.balance(), accountLink);
        }

        return ResponseEntity
                .status(HttpStatusCode.valueOf(409))
                .body(new SuspensionBlockedWarning(debt, groups));
    }
}
