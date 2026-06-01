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

/**
 * Verifies the kit's OpenAPI / Swagger UI auto-configuration over the real HTTP stack:
 * with springdoc on the classpath and no consumer wiring, {@code /v3/api-docs} serves a
 * valid spec (this also catches the springdoc Jackson-2 vs Spring Boot 4 Jackson-3
 * serialization risk — a broken doc would 500 or return non-JSON), {@code /swagger-ui}
 * is reachable, and both are public (not blocked by the admin security chain).
 *
 * <p>The {@code enabled=false} kill-switch is covered by {@link OpenApiDisabledTests}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiEndpointTests {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void apiDocsServesValidJsonWithoutAuth() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs");

        assertThat(response.statusCode()).isEqualTo(200);
        // Real serialization happened (guards against the springdoc/Jackson mismatch):
        // a parseable OpenAPI document with our configured title.
        assertThat(response.body()).contains("\"openapi\"");
        assertThat(response.body()).contains("devslab-kit Admin API");
    }

    @Test
    void adminApiGroupIsExposed() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs/admin");

        assertThat(response.statusCode()).isEqualTo(200);
        // The grouped doc only matches /admin/api/v1/** — its paths should reflect that.
        assertThat(response.body()).contains("/admin/api/v1");
    }

    @Test
    void swaggerUiIsReachableWithoutAuth() throws Exception {
        // springdoc redirects /swagger-ui.html -> /swagger-ui/index.html; either the
        // redirect (3xx) or the page (200) proves it's public, not a 401/403.
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/swagger-ui/index.html"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
