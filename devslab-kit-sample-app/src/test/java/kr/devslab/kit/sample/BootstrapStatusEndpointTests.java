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
 * Verifies the bootstrap-status endpoint (ADR 0001) over the real HTTP stack:
 * it must answer {@code 200 { "initialized": ... }} <em>without</em> a token,
 * since a setup wizard calls it before any account exists.
 *
 * <p>Driven with the JDK's {@link HttpClient} rather than Spring's RestTemplate
 * / TestRestTemplate / MockMvc helpers — Spring Boot 4 relocated all three, and
 * the JDK client has no such churn, doesn't throw on 4xx, and needs no extra
 * test dependency.
 *
 * <p>Runs against the sample-app, whose bootstrap runner provisioned the admin
 * user, so {@code initialized} is {@code true} here.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BootstrapStatusEndpointTests {

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void statusIsReachableWithoutAuthAndReportsInitialized() throws Exception {
        HttpResponse<String> response = get("/admin/api/v1/bootstrap/status");

        assertThat(response.statusCode()).isEqualTo(200);
        // sample-app's bootstrap runner provisioned admin → initialized == true
        assertThat(response.body()).contains("\"initialized\":true");
    }

    @Test
    void otherAdminEndpointsStillRequireAuth() throws Exception {
        // Guard against the permitAll matcher being too broad: a sibling admin
        // endpoint must still reject an unauthenticated request (401/403).
        HttpResponse<String> response = get("/admin/api/v1/users?tenantId=default");

        assertThat(response.statusCode()).isIn(401, 403);
    }
}
