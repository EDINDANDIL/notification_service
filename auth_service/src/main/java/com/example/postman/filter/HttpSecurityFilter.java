package com.example.postman.filter;


import com.example.postman.services.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class HttpSecurityFilter {

    private final JwtBaseFilter jwtBaseFilter;

    private final AuthenticationProvider authenticationProvider;

    private final CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    public HttpSecurityFilter(
            JwtBaseFilter jwtBaseFilter,
            CorsConfigurationSource corsConfigurationSource,
            AuthenticationProvider authenticationProvider,
            CustomOAuth2UserService customOAuth2UserService) {
        this.jwtBaseFilter = jwtBaseFilter;
        this.authenticationProvider = authenticationProvider;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    SecurityFilterChain httpSecurity(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2Client(Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("http://localhost:3000")
                        .defaultSuccessUrl("/auth-success", true)
                        .userInfoEndpoint(userInfoEndpointConfig ->
                                userInfoEndpointConfig.userService(customOAuth2UserService)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/form-login", "/login", "/form-register", "/form-registration", "/auth")
                        .permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtBaseFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
