package ch.ws;

import ch.sthomas.hack.start.data.BaseTestContainer;
import ch.ws.config.WsTestConfig;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = WsTestConfig.class)
@ActiveProfiles({"test", "no-security", "local-output"})
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration
public abstract class BaseWsDataServiceTest extends BaseTestContainer {}
