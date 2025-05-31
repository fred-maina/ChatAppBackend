package com.fredmaina.chatapp.Auth.Repositories;

import com.fredmaina.chatapp.Auth.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);  // Checks if a user with this username exists
    boolean existsByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);

}
