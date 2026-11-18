package com.klabis.members.infrastructure.restapi;

import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberFinancialStatePort;
import com.klabis.members.application.SuspensionBlockedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class MembersExceptionHandler {

    private final ConversionService conversionService;

    MembersExceptionHandler(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @ExceptionHandler(SuspensionBlockedException.class)
    ErrorResponse handleSuspensionBlocked(SuspensionBlockedException ex) {
        LastOwnerWarning groups = lastOwnerWarning(ex.getBlockingGroups());
        OutstandingDebtWarning debt = debtWarning(ex.getDebtSnapshot());

        ErrorResponse.Builder response = ErrorResponse.builder(ex, HttpStatus.CONFLICT, ex.getMessage())
                .title("Conflict")
                .type(URI.create(CommonErrorTypes.uri(ex.errorType())));
        if (groups != null) {
            response.property("groups", groups);
        }
        if (debt != null) {
            response.property("debt", debt);
        }
        if (!ex.getDependents().isEmpty()) {
            response.property("dependentMembers", ex.getDependents().stream()
                    .map(dependent -> DependentMemberWarningBuilder.builder()
                            .memberId(dependent.memberId().uuid())
                            .memberName(dependent.memberName())
                            .groups(lastOwnerWarning(dependent.blockingGroups()))
                            .debt(debtWarning(dependent.debtSnapshot()))
                            .build())
                    .toList());
        }
        return response.build();
    }

    private static LastOwnerWarning lastOwnerWarning(List<OwnedGroup> blockingGroups) {
        if (blockingGroups.isEmpty()) {
            return null;
        }
        List<AffectedGroup> affectedGroups = blockingGroups.stream()
                .map(info -> AffectedGroupBuilder.builder()
                        .groupId(UUID.fromString(info.groupId()))
                        .groupName(info.groupName())
                        .groupType(AffectedGroupGroupType.fromValue(info.groupType()))
                        .groupLink(info.groupLink())
                        .build())
                .toList();
        return LastOwnerWarningBuilder.builder().affectedGroups(affectedGroups).build();
    }

    private OutstandingDebtWarning debtWarning(MemberFinancialStatePort.MemberFinancialSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        URI accountLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/members/{memberId}/account")
                .buildAndExpand(snapshot.memberId().uuid())
                .toUri();
        return OutstandingDebtWarningBuilder.builder()
                .balance(conversionService.convert(snapshot.balance(), MonetaryAmount.class))
                .accountLink(accountLink)
                .build();
    }
}
