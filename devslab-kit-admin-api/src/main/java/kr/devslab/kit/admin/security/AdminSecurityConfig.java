package kr.devslab.kit.admin.security;

import kr.devslab.kit.admin.AdminApiPaths;
import kr.devslab.kit.identity.AuthTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class AdminSecurityConfig {

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthTokenService tokenService) {
        return new JwtAuthenticationFilter(tokenService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain devslabKitAdminSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {
        http
                .securityMatcher(AdminApiPaths.BASE + "/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers(HttpMethod.POST, AdminApiPaths.BASE + "/auth/login").permitAll()
                        // Setup-wizard probe — must answer before any account
                        // exists, so it cannot require authentication. Leaks
                        // only a single boolean (see BootstrapStatusController).
                        .requestMatchers(HttpMethod.GET, AdminApiPaths.BOOTSTRAP_STATUS).permitAll()
                        .requestMatchers(AdminApiPaths.BASE + "/**").authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
