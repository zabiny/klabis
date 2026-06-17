package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.application.CampaignAlreadyProcessedException;
import com.klabis.membershipfees.domain.ActiveCampaignExistsException;
import com.klabis.membershipfees.domain.CampaignClosedException;
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
        return conflict("Duplicate Payment Rule", ex);
    }

    @ExceptionHandler(PaymentRuleNotFoundException.class)
    ResponseEntity<ProblemDetail> handlePaymentRuleNotFound(PaymentRuleNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), ex.getMessage());
        problem.setTitle("Payment Rule Not Found");
        return ResponseEntity.status(404).body(problem);
    }

    @ExceptionHandler(ActiveCampaignExistsException.class)
    ResponseEntity<ProblemDetail> handleActiveCampaignExists(ActiveCampaignExistsException ex) {
        return conflict("Active Campaign Already Exists", ex);
    }

    @ExceptionHandler(CampaignClosedException.class)
    ResponseEntity<ProblemDetail> handleCampaignClosed(CampaignClosedException ex) {
        return conflict("Campaign Closed", ex);
    }

    @ExceptionHandler(CampaignAlreadyProcessedException.class)
    ResponseEntity<ProblemDetail> handleCampaignAlreadyProcessed(CampaignAlreadyProcessedException ex) {
        return conflict("Campaign Already Processed", ex);
    }

    private ResponseEntity<ProblemDetail> conflict(String title, Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(409), ex.getMessage());
        problem.setTitle(title);
        return ResponseEntity.status(409).body(problem);
    }
}
