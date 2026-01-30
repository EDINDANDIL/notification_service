package com.example.postman.services;

import com.example.postman.models.OAuthUser;
import com.example.postman.repositories.OAuthUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Custom OAuth2 user service that loads and saves OAuth user information.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String ID_ATTRIBUTE = "id";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String AVATAR_URL_ATTRIBUTE = "avatar_url";

    private final OAuthUserRepository oAuthUserRepository;

    public CustomOAuth2UserService(OAuthUserRepository oAuthUserRepository) {
        this.oAuthUserRepository = oAuthUserRepository;
    }

    /**
     * Loads OAuth2 user information and saves it to the repository if not exists.
     *
     * @param userRequest the OAuth2 user request
     * @return the OAuth2 user
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthUser user = OAuthUser.builder()
                .provider(registrationId)
                .providerId(Objects.requireNonNull(oAuth2User.getAttribute(ID_ATTRIBUTE)).toString())
                .name(oAuth2User.getAttribute(NAME_ATTRIBUTE))
                .email(oAuth2User.getAttribute(EMAIL_ATTRIBUTE))
                .avatarUri(oAuth2User.getAttribute(AVATAR_URL_ATTRIBUTE))
                .build();

        oAuthUserRepository.findByProviderId(user.getProviderId())
                .orElseGet(() -> oAuthUserRepository.save(user));

        return user;
    }
}