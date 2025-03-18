package ch.sthomas.hack.start.service;

import ch.sthomas.hack.start.data.BaseTestContainer;
import ch.sthomas.hack.start.service.config.ServiceTestConfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = ServiceTestConfig.class)
@ActiveProfiles({"test", "no-security", "local-output"})
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public abstract class BaseServiceDataServiceTest extends BaseTestContainer {}
