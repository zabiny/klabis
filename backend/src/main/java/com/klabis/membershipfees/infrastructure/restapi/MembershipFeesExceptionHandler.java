package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.exceptions.CommonErrorTypes;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.membershipfees.application.MembershipFeeTierOptionsPort;
import com.klabis.membershipfees.domain.DuplicatePaymentRuleException;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.PaymentRuleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Payment rules reference the event type by ID and the ranking by its short name; the user knows them
 * by name, so the names are resolved here, where the options port is available. A name that cannot be
 * resolved is left out, never replaced by the identifier.
 */
@RestControllerAdvice(basePackageClasses = MembershipFeesExceptionHandler.class)
@Order(1)
class MembershipFeesExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MembershipFeesExceptionHandler.class);

    private final MembershipFeeTierOptionsPort optionsPort;

    MembershipFeesExceptionHandler(MembershipFeeTierOptionsPort optionsPort) {
        this.optionsPort = optionsPort;
    }

    @ExceptionHandler(DuplicatePaymentRuleException.class)
    ErrorResponse handleDuplicatePaymentRule(DuplicatePaymentRuleException ex) {
        return toResponse(ex, HttpStatus.CONFLICT, "Conflict", ex.eventTypeId(), ex.rankingShortName());
    }

    @ExceptionHandler(PaymentRuleNotFoundException.class)
    ErrorResponse handlePaymentRuleNotFound(PaymentRuleNotFoundException ex) {
        return toResponse(ex, HttpStatus.NOT_FOUND, "Resource Not Found", ex.eventTypeId(), ex.rankingShortName());
    }

    private <E extends RuntimeException & UserFacingError> ErrorResponse toResponse(E ex, HttpStatus status, String title,
                                     EventTypeReference eventTypeId, String rankingShortName) {
        ErrorResponse.Builder builder = ErrorResponse.builder(ex, status, ex.getMessage())
                .title(title)
                .type(URI.create(CommonErrorTypes.uri(ex.errorType())));
        Map<String, String> names = new LinkedHashMap<>();
        resolve(optionsPort::listEventTypeOptions, eventTypeId.value().toString())
                .ifPresent(name -> names.put("eventTypeName", name));
        resolve(optionsPort::listRankingOptions, rankingShortName)
                .ifPresent(label -> names.put("rankingLabel", label));
        names.forEach(builder::property);
        return builder.build();
    }

    private Optional<String> resolve(Supplier<List<HalFormsInlineOption>> options, String value) {
        try {
            return options.get().stream()
                    .filter(option -> value.equals(option.value()))
                    .map(HalFormsInlineOption::prompt)
                    .findFirst();
        } catch (RuntimeException e) {
            LOG.warn("Name lookup for error message failed", e);
            return Optional.empty();
        }
    }
}
