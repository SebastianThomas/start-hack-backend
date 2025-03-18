package ch.sthomas.hack.start.data.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootConfiguration
@EntityScan("ch.sthomas.hack.start.model.entity")
@EnableJpaRepositories("ch.sthomas.hack.start.data.repository")
@EnableTransactionManagement
public class DataTestConfig {}
