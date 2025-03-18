package ch.sthomas.hack.start.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.zalando.problem.spring.web.autoconfigure.security.ProblemSecurityAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {
            "ch.sthomas.hack.start.data",
            "ch.sthomas.hack.start.service",
            "ch.sthomas.hack.start.ws",
        },
        // exclude ErrorMvcAutoConfiguration and ProblemSecurityAutoConfiguration when using
        // zalando/problem
        exclude = {ErrorMvcAutoConfiguration.class, ProblemSecurityAutoConfiguration.class})
public class StartHackWsApplication {

    public static void main(final String[] args) {
        SpringApplication.run(StartHackWsApplication.class, args);
    }
}
