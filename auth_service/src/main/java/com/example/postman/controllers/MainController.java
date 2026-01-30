package com.example.postman.controllers;

import com.example.postman.models.OAuthUser;
import com.example.postman.repositories.OAuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for handling main application endpoints including authentication checks
 * and user profile image retrieval.
 */
@RestController
public class MainController {

    private static final String DEFAULT_AVATAR_URL = "https://sun9-23.userapi.com/s/v1/if2/8-iRVp5dL179aNIJQphYWD7op5PM9aHxbWXHJ8vTB-yzD-6z6e8d9VYxDkA_HQzT85cXb3_NL0Y1yeL8FV-U6Dl2.jpg?quality=96&as=32x24,48x36,72x54,108x81,160x120,240x180,360x270,480x360,540x405,604x453&from=bu";
    private static final String ID_ATTRIBUTE = "id";

    @Autowired
    private OAuthUserRepository oAuthUserRepository;

    /**
     * Checks if the current user is authenticated.
     *
     * @return response indicating authentication status
     */
    @GetMapping("/auth_check")
    public ResponseEntity<Map<String, String>> checkAuth() {
        return ResponseEntity.ok(Map.of("auth", "success"));
    }

    /**
     * Retrieves the avatar image URL for the authenticated user.
     *
     * @param principal the authenticated principal
     * @return response containing the avatar image URL
     */
    @GetMapping("/image")
    public ResponseEntity<Map<String, String>> getImage(@AuthenticationPrincipal Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            Optional<OAuthUser> oAuthUser = extractOAuthUser(oAuth2User, authentication);
            if (oAuthUser.isPresent() && oAuthUser.get().getAvatarUri() != null) {
                return ResponseEntity.ok(Map.of("image", oAuthUser.get().getAvatarUri()));
            }
        }

        return ResponseEntity.ok(Map.of("image", DEFAULT_AVATAR_URL));
    }

    private Optional<OAuthUser> extractOAuthUser(OAuth2User oAuth2User, Authentication authentication) {
        if (oAuth2User instanceof OAuthUser oAuthUser) {
            return Optional.of(oAuthUser);
        } else if (authentication.getPrincipal() instanceof DefaultOAuth2User defaultOAuth2User) {
            Object idAttribute = defaultOAuth2User.getAttribute(ID_ATTRIBUTE);
            if (idAttribute != null) {
                return oAuthUserRepository.findByProviderId(String.valueOf(idAttribute));
            }
        }
        return Optional.empty();
    }
}
