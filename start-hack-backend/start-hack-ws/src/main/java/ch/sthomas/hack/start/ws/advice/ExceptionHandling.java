package ch.sthomas.hack.start.ws.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class ExceptionHandling extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ProblemDetail> handleNoSuchElement(final NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> handleAuthentication(final AuthenticationException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, exception);
    }

    private static ResponseEntity<ProblemDetail> problem(
            final HttpStatus status, final Exception exception) {
        final var message =
                exception.getMessage() == null ? status.getReasonPhrase() : exception.getMessage();
        final var detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(detail);
    }
}
