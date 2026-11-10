package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.exceptions.UserFacingErrorResponses;
import com.klabis.finance.application.MemberAccountNotFoundException;
import com.klabis.finance.application.TransactionNotFoundException;
import com.klabis.finance.domain.OverdraftLimitExceededException;
import com.klabis.finance.domain.TransactionAlreadyReversedException;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Finance errors are explained to the user by the member's name, but the exceptions only know the
 * member's id, so the name is resolved here. A name that cannot be resolved is left out, never replaced
 * by the identifier.
 */
@RestControllerAdvice(basePackageClasses = FinanceExceptionHandler.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class FinanceExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FinanceExceptionHandler.class);

    private final Members members;

    FinanceExceptionHandler(Members members) {
        this.members = members;
    }

    // Prepares HTTP 422 mapping for the registration-charge REST path (chargeForRegistration),
    // which currently has no caller — pending the event-registration change that will introduce it.
    @ExceptionHandler(OverdraftLimitExceededException.class)
    ErrorResponse handleOverdraftLimitExceeded(OverdraftLimitExceededException ex) {
        return toResponse(ex, ex.memberId(), HttpStatusCode.valueOf(422), "Overdraft Limit Exceeded");
    }

    @ExceptionHandler(TransactionAlreadyReversedException.class)
    ErrorResponse handleTransactionAlreadyReversed(TransactionAlreadyReversedException ex) {
        return toResponse(ex, ex.memberId(), HttpStatus.CONFLICT, "Conflict");
    }

    @ExceptionHandler(MemberAccountNotFoundException.class)
    ErrorResponse handleMemberAccountNotFound(MemberAccountNotFoundException ex) {
        return toResponse(ex, ex.memberId(), HttpStatus.NOT_FOUND, "Resource Not Found");
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    ErrorResponse handleTransactionNotFound(TransactionNotFoundException ex) {
        return toResponse(ex, ex.memberId(), HttpStatus.NOT_FOUND, "Resource Not Found");
    }

    private <E extends RuntimeException & UserFacingError> ErrorResponse toResponse(
            E ex, MemberId memberId, HttpStatusCode status, String title) {
        Map<String, Object> names = new LinkedHashMap<>();
        memberName(memberId).ifPresent(name -> names.put("memberName", name));
        return UserFacingErrorResponses.of(ex, status, title, names);
    }

    private Optional<String> memberName(MemberId memberId) {
        try {
            return members.findById(memberId).map(member -> member.firstName() + " " + member.lastName());
        } catch (RuntimeException e) {
            LOG.warn("Member name lookup for error message failed", e);
            return Optional.empty();
        }
    }
}
