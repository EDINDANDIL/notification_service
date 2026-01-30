package com.example.postman.controllers;

import com.example.postman.exceptions.InvalidJwt;
import com.example.postman.models.BasicUser;
import com.example.postman.models.OAuthUser;
import com.example.postman.repositories.OAuthUserRepository;
import com.example.postman.services.AuthService;
import com.example.postman.services.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.AuthenticationException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling authentication operations including registration, login,
 * OAuth authentication, token refresh, and logout.
 */
@RestController
public class AuthController {

    private static final String FRONTEND_URL = "http://localhost:3000";
    private static final String AUTH_TYPE_COOKIE = "AUTH_TYPE";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    private static final String AUTH_TYPE_BASE = "base";
    private static final String AUTH_TYPE_OAUTH = "oauth";
    private static final int ACCESS_TOKEN_MAX_AGE = 60 * 5;
    private static final long REFRESH_TOKEN_MAX_AGE = 60L * 60 * 24 * 30;
    private static final int REFRESH_ACCESS_TOKEN_MAX_AGE = 60 * 15;

    private final AuthService authService;
    private final OAuthUserRepository oAuthUserRepository;
    private final JwtService jwtService;

    @Autowired
    public AuthController(AuthService authService, OAuthUserRepository oAuthUserRepository, JwtService jwtService) {
        this.authService = authService;
        this.oAuthUserRepository = oAuthUserRepository;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user with basic authentication.
     *
     * @param basicUser the user registration data
     * @param response  the HTTP response to set cookies
     * @return response with success status
     */
    @PostMapping("/form-register")
    public ResponseEntity<Map<String, String>> register(@RequestBody BasicUser basicUser, HttpServletResponse response) {
        Map<String, String> tokens = authService.register(basicUser);

        setAuthCookies(response, tokens.get("access"), tokens.get("refresh"), AUTH_TYPE_BASE);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Authenticates a user with basic credentials.
     *
     * @param basicUser the user login credentials
     * @param response  the HTTP response to set cookies
     * @return response with success status
     */
    @PostMapping("/form-login")
    public ResponseEntity<Map<String, String>> login(@RequestBody BasicUser basicUser, HttpServletResponse response) {
        Map<String, String> tokens = authService.login(basicUser);

        setAuthCookies(response, tokens.get("access"), tokens.get("refresh"), AUTH_TYPE_BASE);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    /**
     * Handles successful OAuth authentication and sets authentication cookies.
     *
     * @param principal                    the authenticated OAuth principal
     * @param response                    the HTTP response to set cookies
     * @param oAuth2AuthenticationToken    the OAuth authentication token
     * @param clientRegistration          the OAuth client registration
     * @return redirect response to frontend login page
     */
    @GetMapping("/auth-success")
    public ResponseEntity<Void> handleAuthSuccess(
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            HttpServletResponse response,
            OAuth2AuthenticationToken oAuth2AuthenticationToken,
            ClientRegistration clientRegistration) {

        OAuthUser oAuthUser = (OAuthUser) principal;

        String accessToken = jwtService.createAccessTokenForOauth(
                oAuthUser.getProviderId(), oAuthUser.getProvider());
        String refreshToken = jwtService.createOathRefreshToken(
                oAuthUser.getProviderId(), oAuthUser.getProvider());

        if (!oAuthUserRepository.existsOAuthUserByProviderIdAndProvider(
                oAuthUser.getProviderId(), oAuthUser.getProvider())) {
            oAuthUserRepository.save(oAuthUser);
        }

        setAuthCookies(response, accessToken, refreshToken, AUTH_TYPE_OAUTH);

        return ResponseEntity.status(302)
                .location(URI.create(FRONTEND_URL + "/login"))
                .build();
    }

    /**
     * Logs out the user by clearing authentication cookies and redirects to frontend.
     *
     * @param response the HTTP response to clear cookies
     * @return redirect response to frontend
     */
    @GetMapping("/logout-and-redirect")
    public ResponseEntity<Void> logoutAndRedirect(HttpServletResponse response) {
        ResponseCookie deleteAccess = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        ResponseCookie deleteRefresh = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", deleteAccess.toString());
        response.addHeader("Set-Cookie", deleteRefresh.toString());

        return ResponseEntity.status(302)
                .header("Location", FRONTEND_URL)
                .build();
    }

    /**
     * Refreshes the access token using the refresh token from cookies.
     *
     * @param request  the HTTP request containing cookies
     * @param response the HTTP response to set new access token cookie
     * @return response with success status or unauthorized if token is invalid
     * @throws AuthenticationException if authentication fails
     * @throws InvalidJwt             if JWT token is invalid
     */
    @GetMapping("/auth")
    public ResponseEntity<Map<String, String>> auth(
            HttpServletRequest request,
            HttpServletResponse response) throws AuthenticationException, InvalidJwt {

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("status", "unauthorized"));
        }

        String refreshToken = extractCookieValue(cookies, REFRESH_TOKEN_COOKIE);
        String authType = extractCookieValue(cookies, AUTH_TYPE_COOKIE);

        if (refreshToken == null || !jwtService.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("status", "unauthorized"));
        }

        String accessToken = createAccessTokenFromRefreshToken(refreshToken, authType);
        if (accessToken == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("status", "unauthorized"));
        }

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_ACCESS_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, String authType) {
        ResponseCookie authTypeCookie = ResponseCookie.from(AUTH_TYPE_COOKIE, authType)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .build();

        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, accessToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        response.addHeader("Set-Cookie", authTypeCookie.toString());
    }

    private String extractCookieValue(Cookie[] cookies, String cookieName) {
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String createAccessTokenFromRefreshToken(String refreshToken, String authType) {
        if (AUTH_TYPE_OAUTH.equals(authType)) {
            List<String> oauthSubjects = jwtService.getOauthSubjects(refreshToken);
            if (oauthSubjects.size() < 2) {
                return null;
            }
            return jwtService.createAccessTokenForOauth(oauthSubjects.get(0), oauthSubjects.get(1));
        } else if (AUTH_TYPE_BASE.equals(authType)) {
            String username = jwtService.getBaseSubject(refreshToken);
            if (username == null) {
                return null;
            }
            return jwtService.createBaseAccessToken(username);
        }
        return null;
    }
}
