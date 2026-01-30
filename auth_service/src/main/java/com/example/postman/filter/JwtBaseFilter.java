package com.example.postman.filter;

import com.example.postman.models.BasicUser;
import com.example.postman.models.OAuthUser;
import com.example.postman.repositories.BasicUserRepository;
import com.example.postman.repositories.OAuthUserRepository;
import com.example.postman.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
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

    private static final String AUTH_TYPE_COOKIE = "AUTH_TYPE";
    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    private static final String AUTH_TYPE_OAUTH = "oauth";
    private static final String AUTH_TYPE_BASE = "base";
    private static final String AUTH_ENDPOINT = "/auth";

    private final JwtService jwtService;
    private final OAuthUserRepository oAuthUserRepository;
    private final BasicUserRepository basicUserRepository;

    @Autowired
    public JwtBaseFilter(
            JwtService jwtService,
            OAuthUserRepository oAuthUserRepository,
            BasicUserRepository basicUserRepository) {
        this.jwtService = jwtService;
        this.oAuthUserRepository = oAuthUserRepository;
        this.basicUserRepository = basicUserRepository;
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

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cookie> authTypeOptional = findCookie(cookies, AUTH_TYPE_COOKIE);
        Optional<Cookie> accessTokenOptional = findCookie(cookies, ACCESS_TOKEN_COOKIE);

        if (authTypeOptional.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (accessTokenOptional.isEmpty()) {
            handleRefreshTokenOnly(cookies, request, response, filterChain);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authType = authTypeOptional.get().getValue();
        String accessToken = accessTokenOptional.get().getValue();

        if (!jwtService.validateToken(accessToken)) {
            handleInvalidToken(request, response, filterChain);
            return;
        }

        if (AUTH_TYPE_OAUTH.equals(authType)) {
            handleOAuthAuthentication(cookies, accessToken);
        } else if (AUTH_TYPE_BASE.equals(authType)) {
            handleBasicAuthentication(accessToken);
        }

        filterChain.doFilter(request, response);
    }

    private void handleRefreshTokenOnly(
            Cookie[] cookies,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        Optional<Cookie> refreshTokenOptional = findCookie(cookies, REFRESH_TOKEN_COOKIE);
        if (refreshTokenOptional.isPresent() && jwtService.validateToken(refreshTokenOptional.get().getValue())) {
            if (AUTH_ENDPOINT.equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void handleInvalidToken(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        if (AUTH_ENDPOINT.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void handleOAuthAuthentication(Cookie[] cookies, String accessToken) {
        try {
            List<String> oauthSubjects = jwtService.getOauthSubjects(accessToken);
            if (oauthSubjects.size() < 2) {
                return;
            }

            Optional<OAuthUser> oAuthUserOptional = oAuthUserRepository.findOAuthUserByProviderIdAndProvider(
                    oauthSubjects.get(0), oauthSubjects.get(1));

            if (oAuthUserOptional.isPresent()) {
                OAuthUser user = oAuthUserOptional.get();
                Authentication auth = new OAuth2AuthenticationToken(
                        user,
                        user.getAuthorities(),
                        user.getProviderId()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            // Authentication failed, continue without setting security context
        }
    }

    private void handleBasicAuthentication(String accessToken) {
        try {
            String username = jwtService.getBaseSubject(accessToken);
            if (username == null) {
                return;
            }

            Optional<BasicUser> basicUserOptional = basicUserRepository.findBasicUserByUsername(username);

            if (basicUserOptional.isPresent()) {
                BasicUser user = basicUserOptional.get();
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        user.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            // Authentication failed, continue without setting security context
        }
    }

    private Optional<Cookie> findCookie(Cookie[] cookies, String name) {
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst();
    }
}

