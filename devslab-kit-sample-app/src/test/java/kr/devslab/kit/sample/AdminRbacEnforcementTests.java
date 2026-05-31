package kr.devslab.kit.sample;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;
import kr.devslab.kit.core.id.PublicId;
import kr.devslab.kit.core.id.TenantId;
import kr.devslab.kit.core.id.UserId;
import kr.devslab.kit.identity.AuthTokenService;
import kr.devslab.kit.identity.CurrentUser;
import kr.devslab.kit.identity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Proves the admin REST API enforces {@code admin.*} permissions out of the box — the
 * documented promise ("guarded by Spring Security and the kit's permissions") actually holds
 * for a consumer that only added the starter.
 *
 * <ul>
 *   <li><b>allow</b> — the bootstrap admin (seeded with every {@code admin.*} permission on
 *       {@code PLATFORM_ADMIN}) logs in and a protected read endpoint returns 200;</li>
 *   <li><b>deny</b> — an authenticated caller holding <em>no</em> grants gets 403 on the same
 *       endpoint (so authentication alone is not enough — the permission is enforced).</li>
 * </ul>
 *
 * The unauthenticated case (401/403 with no token) is covered by
 * {@link BootstrapStatusEndpointTests#otherAdminEndpointsStillRequireAuth()}.
 *
 * <p>Driven with the JDK {@link HttpClient} against the real HTTP stack (no MockMvc), matching
 * {@link BootstrapStatusEndpointTests}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminRbacEnforcementTests {

    private static final String USERS = "/admin/api/v1/users?tenantId=default";
    private static final String LOGIN = "/admin/api/v1/auth/login";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTokenService tokenService;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void seededAdminHoldsEveryPermissionAndCanReadUsers() throws Exception {
        String adminToken = login("admin", "admin");

        HttpResponse<String> response = getWithToken(USERS, adminToken);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void authenticatedCallerWithoutThePermissionIsForbidden() throws Exception {
        // A syntactically valid, correctly signed token (issued by the app's own
        // AuthTokenService) for a principal that holds no role/group grants — so the
        // per-request permission resolution finds nothing and the hasAuthority("admin.user.read")
        // rule denies. The user id is random, hence zero grants in the database.
        CurrentUser noGrants = new CurrentUser(
                UserId.of(UUID.randomUUID()),
                PublicId.of("pub-nobody"),
                TenantId.of("default"),
                "nobody",
                UserStatus.ACTIVE,
                Set.of(),
                false);
        String token = tokenService.issue(noGrants).value();

        HttpResponse<String> response = getWithToken(USERS, token);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private String login(String loginId, String password) throws Exception {
        HttpResponse<String> response = post(LOGIN, loginBody(loginId, password));
        assertThat(response.statusCode()).isEqualTo(200);
        return objectMapper.readTree(response.body()).get("token").asText();
    }

    private static String loginBody(String loginId, String password) {
        return "{\"tenantId\":\"default\",\"loginId\":\"" + loginId + "\",\"rawPassword\":\"" + password + "\"}";
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithToken(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
