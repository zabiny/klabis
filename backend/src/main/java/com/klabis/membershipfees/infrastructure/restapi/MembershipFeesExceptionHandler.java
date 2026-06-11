package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.DuplicatePaymentRuleException;
import com.klabis.membershipfees.domain.PaymentRuleNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(1)
class MembershipFeesExceptionHandler {

    @ExceptionHandler(DuplicatePaymentRuleException.class)
    ResponseEntity<ProblemDetail> handleDuplicatePaymentRule(DuplicatePaymentRuleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(409), ex.getMessage());
        problem.setTitle("Duplicate Payment Rule");
        return ResponseEntity.status(409).body(problem);
    }

    @ExceptionHandler(PaymentRuleNotFoundException.class)
    ResponseEntity<ProblemDetail> handlePaymentRuleNotFound(PaymentRuleNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), ex.getMessage());
        problem.setTitle("Payment Rule Not Found");
        return ResponseEntity.status(404).body(problem);
    }
}
