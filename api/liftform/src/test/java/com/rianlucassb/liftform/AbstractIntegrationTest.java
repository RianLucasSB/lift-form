package com.rianlucassb.liftform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rianlucassb.liftform.infraestructure.config.S3TestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Import({AbstractIntegrationTest.ContainerConfig.class, S3TestConfig.class})
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    // Keep one Postgres and one LocalStack instance alive for the whole test JVM
    // so Spring's cached ApplicationContext never points to a dead mapped port.
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"));

    static final LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
                    .withServices(S3);

    static {
        postgres.start();
        localStack.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("aws.endpoint", () -> localStack.getEndpointOverride(S3).toString());
        registry.add("aws.region", localStack::getRegion);
    }

    @TestConfiguration
    static class ContainerConfig {
        @Bean
        public LocalStackContainer localStackContainer() {
            return localStack;
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
