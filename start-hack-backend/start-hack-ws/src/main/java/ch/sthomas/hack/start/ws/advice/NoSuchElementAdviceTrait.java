package ch.sthomas.hack.start.ws.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import org.zalando.problem.spring.web.advice.AdviceTrait;

import java.util.NoSuchElementException;

public interface NoSuchElementAdviceTrait extends AdviceTrait {

    @ExceptionHandler
    default ResponseEntity<Problem> handleNoSuchElement(
            final NoSuchElementException exception, final NativeWebRequest request) {
        return create(Status.NOT_FOUND, exception, request);
    }

    @ExceptionHandler
    default ResponseEntity<Problem> handleNoSuchElement(
            final NoResourceFoundException exception, final NativeWebRequest request) {
        return create(Status.NOT_FOUND, exception, request);
    }
}
