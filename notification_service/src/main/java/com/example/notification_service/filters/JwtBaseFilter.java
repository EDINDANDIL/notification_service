package com.example.notification_service.filters;

import com.example.notification_service.models.BasicUser;
import com.example.notification_service.models.OAuthUser;
import com.example.notification_service.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Filter for JWT-based authentication that processes access tokens from cookies
 * and sets authentication context for both OAuth and basic authentication.
 */
@Component
public class JwtBaseFilter extends OncePerRequestFilter {

    private static final String TEST_ENDPOINT = "/test";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String AUTH_TYPE_COOKIE = "AUTH_TYPE";
    private static final String AUTH_TYPE_OAUTH = "oauth";
    private static final String AUTH_TYPE_BASE = "base";
    private static final String ROLE_USER = "ROLE_USER";

    private final JwtService jwtService;

    @Autowired
    public JwtBaseFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Processes JWT tokens from cookies and sets authentication context.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if servlet error occurs
     * @throws IOException      if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().contains(TEST_ENDPOINT)) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cookie> accessCookieOptional = findCookie(cookies, ACCESS_TOKEN_COOKIE);
        Optional<Cookie> authTypeOptional = findCookie(cookies, AUTH_TYPE_COOKIE);

        if (accessCookieOptional.isEmpty() || authTypeOptional.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = accessCookieOptional.get().getValue();
        if (!jwtService.validateToken(accessToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String authType = authTypeOptional.get().getValue();
        if (AUTH_TYPE_OAUTH.equals(authType)) {
            authenticateViaOAuth(accessToken);
        } else if (AUTH_TYPE_BASE.equals(authType)) {
            authenticateViaBasic(accessToken);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateViaOAuth(String accessToken) {
        List<String> subjects = jwtService.getOauthSubjects(accessToken);

        if (subjects.size() != 2) {
            return;
        }

        OAuthUser oAuthUser = OAuthUser.builder()
                .providerId(subjects.get(0))
                .provider(subjects.get(1))
                .build();

        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                oAuthUser, null, oAuthUser.getProviderId());
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private void authenticateViaBasic(String accessToken) {
        String username = jwtService.getBaseSubject(accessToken);
        if (username == null) {
            return;
        }

        BasicUser basicUser = new BasicUser();
        basicUser.setUsername(username);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                basicUser,
                null,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Optional<Cookie> findCookie(Cookie[] cookies, String name) {
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst();
    }
}
