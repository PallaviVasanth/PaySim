package com.paysim.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paysim.common.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless HTTP Basic security with two roles:
 * OPERATOR (create + refund + read) and VIEWER (read only).
 *
 * <p>CSRF protection is disabled deliberately: the API is stateless, uses no cookies and requires an
 * Authorization header, so a cross-site form post cannot authenticate.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/demo/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments", "/api/v1/payments/*/refund").hasRole("OPERATOR")
                        .requestMatchers("/api/**").hasAnyRole("OPERATOR", "VIEWER")
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized(mapper)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized(mapper))
                        .accessDeniedHandler(forbidden(mapper)));
        return http.build();
    }

    /** JSON 401 without a WWW-Authenticate header, so browsers do not show a native login popup. */
    private AuthenticationEntryPoint unauthorized(ObjectMapper mapper) {
        return (request, response, ex) -> writeError(mapper, response, request.getRequestURI(),
                HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "UNAUTHENTICATED",
                "Valid credentials are required.");
    }

    private AccessDeniedHandler forbidden(ObjectMapper mapper) {
        return (request, response, ex) -> writeError(mapper, response, request.getRequestURI(),
                HttpServletResponse.SC_FORBIDDEN, "Forbidden", "FORBIDDEN",
                "Your role is not allowed to perform this operation.");
    }

    private static void writeError(ObjectMapper mapper, HttpServletResponse response, String path,
                                   int status, String reason, String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiError.of(status, reason, code, message, path));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${paysim.security.operator-username}") String operatorUser,
            @Value("${paysim.security.operator-password}") String operatorPassword,
            @Value("${paysim.security.viewer-username}") String viewerUser,
            @Value("${paysim.security.viewer-password}") String viewerPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(operatorUser).password(encoder.encode(operatorPassword)).roles("OPERATOR").build(),
                User.withUsername(viewerUser).password(encoder.encode(viewerPassword)).roles("VIEWER").build());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${paysim.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Idempotent-Replayed", "Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
