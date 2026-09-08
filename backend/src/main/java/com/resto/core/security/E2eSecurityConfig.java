package com.resto.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Security configuration for the E2E test profile.
 *
 * <p>Active only when the Spring profile {@code e2e} is set (CI E2E pipeline).
 * Permits all requests without JWT validation because no Keycloak instance
 * is available in the docker-compose.test.yml environment.
 *
 * <p>This class intentionally omits {@code .oauth2ResourceServer(...)} so that
 * no {@link org.springframework.security.oauth2.jwt.JwtDecoder} bean is required.
 */
@Configuration
@EnableWebSecurity
@Profile("e2e")
public class E2eSecurityConfig implements WebMvcConfigurer {

    private final RlsContextInterceptor rlsContextInterceptor;

    public E2eSecurityConfig(RlsContextInterceptor rlsContextInterceptor) {
        this.rlsContextInterceptor = rlsContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rlsContextInterceptor);
    }

    @Bean
    public SecurityFilterChain e2eFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // No auth required in E2E environment
            );

        return http.build();
    }
}
