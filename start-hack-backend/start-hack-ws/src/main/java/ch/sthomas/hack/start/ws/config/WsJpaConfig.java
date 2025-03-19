package ch.sthomas.hack.start.ws.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan("ch.sthomas.hack.start.model.entity")
@EnableJpaRepositories("ch.sthomas.hack.start.data.repository")
@EnableTransactionManagement
@Configuration
public class WsJpaConfig {}
