package ch.sthomas.hack.start.ws.advice;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zalando.problem.spring.web.advice.ProblemHandling;
import org.zalando.problem.spring.web.advice.security.SecurityAdviceTrait;

@RestControllerAdvice
public class ExceptionHandling
        implements ProblemHandling, SecurityAdviceTrait, NotFoundAdviceTrait {}
