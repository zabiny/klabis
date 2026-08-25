package com.klabis.members.infrastructure.restapi;

record SuspensionBlockedWarning(MembersExceptionHandler.OutstandingDebtWarning debt,
                                 MembersExceptionHandler.LastOwnerWarning groups) {
}
