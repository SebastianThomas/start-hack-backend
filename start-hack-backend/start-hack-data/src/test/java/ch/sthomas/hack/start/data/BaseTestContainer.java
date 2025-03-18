package ch.sthomas.hack.start.data;

import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
{%- if "postgresql" in database_type %}
import org.testcontainers.containers.PostgreSQLContainer;
{%- elif "mysql" in database_type %}
import org.testcontainers.containers.MySQLContainer;
{%- elif "mariadb" in database_type %}
import org.testcontainers.containers.MariaDBContainer;
{%- endif -%}
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Transactional
@ContextConfiguration
@AutoConfigureTestEntityManager
public class BaseTestContainer {

    @ServiceConnection
    {%- if "postgresql" in database_type and not postgis %}
    static PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:{{latest_docker_tag(docker_image='postgres')}}"));
    {%- elif "postgresql" in database_type and postgis %}
    static PostgreSQLContainer<?> postgisContainer =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:{{latest_docker_tag(docker_image='postgis/postgis', regex_pattern='^[0-9]+-master$')}}")
                            .asCompatibleSubstituteFor("postgres"));
    {%- elif "mysql" in database_type %}
    static MySQLContainer<?> mySQLContainer =
            new MySQLContainer<>(DockerImageName.parse("mysql:{{latest_docker_tag(docker_image='mysql', regex_pattern='^[0-9]+.[0-9]+$')}}"));
    {%- elif "mariadb" in database_type %}
    static MariaDBContainer<?> mariaDBContainer =
            new MariaDBContainer<>(DockerImageName.parse("mariadb:{{latest_docker_tag(docker_image='mariadb', regex_pattern='^[0-9]+.[0-9]+$')}}"));
    {%- endif -%}

    static {
        {%- if "postgresql" in database_type and not postgis %}
        postgresContainer.start();
        {%- elif "postgresql" in database_type and postgis %}
        postgisContainer.start();
        {%- elif "mysql" in database_type %}
        mySQLContainer.start();
        {%- elif "mariadb" in database_type %}
        mariaDBContainer.start();
        {%- endif -%}
    }
}
