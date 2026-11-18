package ch.sthomas.hack.start.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
            "ch.sthomas.hack.start.data",
            "ch.sthomas.hack.start.service",
            "ch.sthomas.hack.start.ws",
        })
public class StartHackWsApplication {

    public static void main(final String[] args) {
        SpringApplication.run(StartHackWsApplication.class, args);
    }
}
