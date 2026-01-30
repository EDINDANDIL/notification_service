package com.example.postman.services;

import com.example.postman.models.BasicUser;
import com.example.postman.repositories.BasicUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for handling user authentication operations including registration and login.
 */
@Service
public class AuthService {

    private final BasicUserRepository basicUserRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            BasicUserRepository basicUserRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.basicUserRepository = basicUserRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user with encoded password and generates JWT tokens.
     *
     * @param basicUser the user to register
     * @return map containing access and refresh tokens
     * @throws BadCredentialsException if username is already in use
     */
    public Map<String, String> register(BasicUser basicUser) {
        if (basicUserRepository.existsByUsername(basicUser.getUsername())) {
            throw new BadCredentialsException("Username is already in use");
        }

        basicUser.setPassword(passwordEncoder.encode(basicUser.getPassword()));
        basicUserRepository.save(basicUser);

        String accessToken = jwtService.createBaseAccessToken(basicUser.getUsername());
        String refreshToken = jwtService.createBaseRefreshToken(basicUser.getUsername());

        return Map.of("access", accessToken, "refresh", refreshToken);
    }

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * @param basicUser the user credentials
     * @return map containing access and refresh tokens
     * @throws UsernameNotFoundException if user is not found
     * @throws BadCredentialsException   if authentication fails
     */
    public Map<String, String> login(BasicUser basicUser) {
        basicUserRepository.findBasicUserByUsername(basicUser.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("no user with such username"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        basicUser.getUsername(),
                        basicUser.getPassword()
                )
        );

        String accessToken = jwtService.createBaseAccessToken(basicUser.getUsername());
        String refreshToken = jwtService.createBaseRefreshToken(basicUser.getUsername());

        return Map.of("access", accessToken, "refresh", refreshToken);
    }
}
