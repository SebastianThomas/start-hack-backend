package ch.sthomas.hack.start.ws.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.zalando.problem.jackson.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
@EntityScan("ch.sthomas.hack.start.model.entity")
@EnableJpaRepositories("ch.sthomas.hack.start.data.repository")
@EnableTransactionManagement
public class WsBaseConfig {

    @Bean
    ObjectMapper objectMapper() {
        final var javaTimeModule = new JavaTimeModule();
        final var localDateSerializer =
                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        javaTimeModule.addSerializer(LocalDate.class, localDateSerializer);

        return JsonMapper.builder()
                .addModule(javaTimeModule)
                // ParanamerModule allows using @JsonCreator without needing @JsonProperty
                .addModule(new ParanamerModule())
                // Jdk8Module supports Optionals
                .addModule(new Jdk8Module())
                .addModule(new ProblemModule())
                .addModule(new ConstraintViolationProblemModule())
                .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .build();
    }
}
