package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies the bootstrap-status endpoint (ADR 0001) over the real HTTP stack:
 * it must answer {@code 200 { "initialized": ... }} <em>without</em> a token,
 * since a setup wizard calls it before any account exists.
 *
 * <p>Runs against the sample-app, whose bootstrap runner has provisioned the
 * admin user — so {@code initialized} is {@code true} here. The contract that
 * matters for the wizard is that the call succeeds unauthenticated and returns
 * the boolean shape; the value itself just reflects the seeded state.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BootstrapStatusEndpointTests {

    @LocalServerPort
    private int port;

    private final RestTemplate rest = new RestTemplateBuilder().build();

    @Test
    void statusIsReachableWithoutAuthAndReportsInitialized() {
        ResponseEntity<BootstrapStatusBody> response = rest.getForEntity(
                "http://localhost:" + port + "/admin/api/v1/bootstrap/status",
                BootstrapStatusBody.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // sample-app's bootstrap runner provisioned admin → initialized == true
        assertThat(response.getBody().initialized()).isTrue();
    }

    @Test
    void otherAdminEndpointsStillRequireAuth() {
        // Guard against the permitAll matcher being too broad: a sibling admin
        // endpoint must still reject an unauthenticated request. RestTemplate
        // throws on 4xx, so assert on the thrown status.
        assertThatThrownBy(() -> rest.getForEntity(
                "http://localhost:" + port + "/admin/api/v1/users?tenantId=default",
                String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .extracting(ex -> ((HttpClientErrorException) ex).getStatusCode().value())
                .satisfies(code -> assertThat((Integer) code).isIn(401, 403));
    }

    record BootstrapStatusBody(boolean initialized) {
    }
}
