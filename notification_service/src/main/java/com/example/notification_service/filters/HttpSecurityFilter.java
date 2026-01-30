package com.example.notification_service.filters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuration for HTTP security and authentication filters.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class HttpSecurityFilter {

    private static final String TEST_ENDPOINT = "/test";

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtBaseFilter jwtBaseFilter;

    @Autowired
    public HttpSecurityFilter(CorsConfigurationSource corsConfigurationSource, JwtBaseFilter jwtBaseFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtBaseFilter = jwtBaseFilter;
    }

    /**
     * Configures the security filter chain.
     *
     * @param http the HTTP security builder
     * @return the configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(customizer -> customizer.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(TEST_ENDPOINT).permitAll();
                    request.anyRequest().authenticated();
                })
                .addFilterBefore(jwtBaseFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
