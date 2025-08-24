package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.EventException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
class EventExceptionsHandler {

    @ExceptionHandler(EventException.class)
    public ErrorResponse handleEventException(EventException error) {
        return ErrorResponse.builder(error, HttpStatusCode.valueOf(400), error.getMessage()).build();
    }

}
