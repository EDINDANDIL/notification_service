package com.example.postman.models;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.*;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OAuthUser implements OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String name;

    private String provider;

    @Column(nullable = false)
    private String avatarUri;

    @Column(nullable = true)
    private String email;


    @Override
    public Map<String, Object> getAttributes() {

        Map<String, Object> attributes = new HashMap<>();



               attributes.put("providerId", this.providerId);
                attributes.put("provider", this.provider);
                attributes.put("name", this.name);
                attributes.put("avatarUri", this.avatarUri);
                attributes.put("email", this.email);


    return attributes;

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("User"));
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        OAuthUser oAuthUser = (OAuthUser) o;
        return getId() != null && Objects.equals(getId(), oAuthUser.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
