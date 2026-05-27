package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.users.Authority;
import org.springframework.security.core.context.SecurityContextHolder;

final class FinanceSecurityHelper {

    private FinanceSecurityHelper() {
    }

    static boolean callerHasFinanceManage() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> Authority.FINANCE_MANAGE.getValue().equals(a.getAuthority()));
    }
}
