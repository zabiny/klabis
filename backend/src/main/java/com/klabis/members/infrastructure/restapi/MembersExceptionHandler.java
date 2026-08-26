package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.SuspensionBlockedException;
import org.springframework.core.convert.ConversionService;
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

    private final ConversionService conversionService;

    MembersExceptionHandler(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @ExceptionHandler(SuspensionBlockedException.class)
    ResponseEntity<SuspensionBlockedWarning> handleSuspensionBlocked(SuspensionBlockedException ex) {
        LastOwnerWarning groups = null;
        if (!ex.getBlockingGroups().isEmpty()) {
            List<AffectedGroup> affectedGroups = ex.getBlockingGroups().stream()
                    .map(info -> AffectedGroupBuilder.builder()
                            .groupId(UUID.fromString(info.groupId()))
                            .groupName(info.groupName())
                            .groupType(AffectedGroupGroupType.fromValue(info.groupType()))
                            .build())
                    .toList();
            groups = LastOwnerWarningBuilder.builder()
                    .message("Member is the last owner of %d group(s) — designate a successor before suspension"
                            .formatted(affectedGroups.size()))
                    .affectedGroups(affectedGroups)
                    .build();
        }

        OutstandingDebtWarning debt = null;
        if (ex.getDebtSnapshot() != null) {
            var snapshot = ex.getDebtSnapshot();
            URI accountLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/members/{memberId}/account")
                    .buildAndExpand(snapshot.memberId().uuid())
                    .toUri();
            debt = OutstandingDebtWarningBuilder.builder()
                    .balance(conversionService.convert(snapshot.balance(), MonetaryAmount.class))
                    .accountLink(accountLink)
                    .build();
        }

        return ResponseEntity
                .status(HttpStatusCode.valueOf(409))
                .body(SuspensionBlockedWarningBuilder.builder().debt(debt).groups(groups).build());
    }
}
