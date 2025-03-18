package ch.sthomas.hack.start.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * You can just delete this test later, especially if fails because a newer database version
 * docker image was downloaded and the major version has mismatch. This test exists mainly to
 * validate the test setup on template instantiation.
 */
class SetupDataServiceTest extends BaseDataServiceTest {
    @Autowired private JdbcClient jdbcClient;

    @Test
    void testSetup() {
        {%- if "postgresql" in database_type %}
        final var version =
                jdbcClient
                        .sql("SELECT split_part(split_part(version(), ' ', 2), '.', 1) AS major_version")
                        .query(String.class)
                        .single();
        assertEquals("16", version);
        {%- elif "mysql" in database_type %}
        final var version =
                jdbcClient
                        .sql("SELECT SUBSTRING_INDEX(VERSION(), '.', 1) AS major_version")
                        .query(String.class)
                        .single();
        assertEquals("8", version);
        {%- elif "mariadb" in database_type %}
        final var version =
                jdbcClient
                        .sql("SELECT SUBSTRING_INDEX(VERSION(), '.', 1) AS major_version")
                        .query(String.class)
                        .single();
        assertEquals("10", version);
        {%- endif %}
    }
}
