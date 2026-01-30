package com.example.postman.configurations;

import com.example.postman.repositories.BasicUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for authentication components including user details service,
 * authentication provider, password encoder, and authentication manager.
 */
@Configuration
public class AuthConfiguration {

    private final BasicUserRepository basicUserRepository;

    @Autowired
    public AuthConfiguration(BasicUserRepository basicUserRepository) {
        this.basicUserRepository = basicUserRepository;
    }

    /**
     * Configures the user details service for loading users by username.
     *
     * @return the user details service
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> basicUserRepository.findBasicUserByUsername(username).orElseThrow();
    }

    /**
     * Configures the authentication provider with password encoder and user details service.
     *
     * @return the authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        daoAuthenticationProvider.setUserDetailsService(userDetailsService());
        return daoAuthenticationProvider;
    }

    /**
     * Configures the password encoder using BCrypt.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the authentication manager.
     *
     * @param authenticationConfiguration the authentication configuration
     * @return the authentication manager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
