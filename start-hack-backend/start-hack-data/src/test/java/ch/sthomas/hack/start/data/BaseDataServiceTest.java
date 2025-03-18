package ch.sthomas.hack.start.data;

import ch.sthomas.hack.start.data.config.DataTestConfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = DataTestConfig.class)
@ActiveProfiles({"test", "no-security", "local-output"})
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public abstract class BaseDataServiceTest extends BaseTestContainer {}
