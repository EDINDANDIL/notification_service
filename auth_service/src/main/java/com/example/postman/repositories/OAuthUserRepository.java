package com.example.postman.repositories;

import com.example.postman.models.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthUserRepository extends JpaRepository<OAuthUser, Long> {
    Optional<OAuthUser> findOAuthUserByProviderIdAndProvider(String id, String provider);
    Optional<OAuthUser> findByProviderId(String providerId);
    boolean existsOAuthUserByProviderIdAndProvider(String id, String provider);
}
