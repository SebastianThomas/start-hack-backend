package ch.sthomas.hack.start.ws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

class PackagedApplicationIT {

    @Test
    void packagedJarContainsTheProductionSecurityRuntime() throws IOException {
        final var packagedJar = Path.of(System.getProperty("packagedJar"));
        final Set<String> entries;
        try (final var jar = new ZipFile(packagedJar.toFile())) {
            entries =
                    jar.stream()
                            .map(entry -> entry.getName())
                            .collect(java.util.stream.Collectors.toSet());
        }

        assertContainsLibrary(entries, "spring-security-config");
        assertContainsLibrary(entries, "spring-security-crypto");
        assertContainsLibrary(entries, "spring-security-oauth2-resource-server");
        assertContainsLibrary(entries, "spring-security-web");
        assertFalse(
                entries.stream().anyMatch(name -> name.contains("problem-spring-web")),
                "The unsupported problem-spring-web starter must not be packaged");
    }

    private static void assertContainsLibrary(final Set<String> entries, final String artifactId) {
        assertTrue(
                entries.stream()
                        .anyMatch(
                                name ->
                                        name.startsWith("BOOT-INF/lib/" + artifactId + "-")
                                                && name.endsWith(".jar")),
                () -> artifactId + " is missing from the packaged application");
    }
}
