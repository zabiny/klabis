package com.klabis.members.infrastructure.restapi;

import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.application.LegalGuardianPort.GuardianInput;
import com.klabis.members.legalguardian.application.LegalGuardianPort.NewLegalGuardian;

import java.util.List;

public final class GuardianInputMapper {

    private GuardianInputMapper() {
    }

    public static List<GuardianInput> toInputs(List<LegalGuardianInputRequest> items) {
        return items == null ? List.of() : items.stream().map(GuardianInputMapper::toInput).toList();
    }

    public static GuardianInput toInput(LegalGuardianInputRequest item) {
        boolean hasNewGuardianData = item.firstName() != null || item.lastName() != null
                                     || item.email() != null || item.phone() != null;
        if ((item.userId() == null) == !hasNewGuardianData) {
            throw new IllegalArgumentException(
                    "Legal guardian must be given either by userId or by the details of a new guardian");
        }
        if (item.userId() != null) {
            return GuardianInput.existing(new UserId(item.userId()));
        }
        return GuardianInput.created(new NewLegalGuardian(item.firstName(), item.lastName(), item.email(), item.phone()));
    }
}
