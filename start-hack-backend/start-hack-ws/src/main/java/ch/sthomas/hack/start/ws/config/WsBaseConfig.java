package ch.sthomas.hack.start.ws.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class WsBaseConfig {

    @Bean
    @Primary
    ObjectMapper objectMapper(final GeometryFactory wgs84GeometryFactory) {
        final var localDateModule = new SimpleModule();
        final var localDateSerializer =
                new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        localDateModule.addSerializer(LocalDate.class, localDateSerializer);

        return JsonMapper.builder()
                .addModule(localDateModule)
                .addModule(new JtsModule(wgs84GeometryFactory))
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .build();
    }

    @Bean
    public GeometryFactory wgs84GeometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
}
