package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Proves the kill-switch: {@code devslab.kit.openapi.enabled=false} turns off the kit's
 * OpenAPI wiring even with springdoc on the classpath. The kit's grouped admin doc must
 * be gone; springdoc's own bare default may still answer {@code /v3/api-docs}, so this
 * asserts the kit-specific group ({@code /v3/api-docs/admin}) is absent rather than that
 * springdoc is fully silenced.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "devslab.kit.openapi.enabled=false")
class OpenApiDisabledTests {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void adminGroupIsAbsentWhenDisabled() throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/v3/api-docs/admin"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        // No kit-defined "admin" group -> springdoc has no such group -> not 200.
        assertThat(response.statusCode()).isNotEqualTo(200);
    }
}
