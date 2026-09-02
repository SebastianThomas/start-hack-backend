package ch.sthomas.hack.start.ws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WsMvcConfig implements WebMvcConfigurer {

    private final String publicFolder;

    public WsMvcConfig(
            @Value("${ch.sthomas.hack.start.public.folder}") final String publicFolder) {
        this.publicFolder = publicFolder;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(
                MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.ALL);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve the derived geojson the frontend fetches (formerly the
        // start-hack-public nginx vhost - now soil-metrics-public.sthomas.ch,
        // path-rewritten to /public, and soil-metrics-web.sthomas.ch/public/*).
        // WsSchedulingConfig regenerates these into publicFolder on startup.
        // spring.web.resources.add-mappings=false only disables the *default*
        // handlers, not this explicit one.
        registry.addResourceHandler("/public/**")
                .addResourceLocations("file:" + publicFolder + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // The public datasets are meant to be fetched cross-origin (the legacy
        // start-hack-public vhost sent Access-Control-Allow-Origin: *).
        registry.addMapping("/public/**").allowedOrigins("*").allowedMethods("GET", "HEAD");
    }
}
