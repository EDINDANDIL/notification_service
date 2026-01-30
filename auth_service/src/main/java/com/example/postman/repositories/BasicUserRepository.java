package com.example.postman.repositories;

import com.example.postman.models.BasicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface BasicUserRepository extends JpaRepository<BasicUser, Long> {

    Optional<BasicUser> findBasicUserByUsername(String username);
    boolean existsByUsername(String login);

}
