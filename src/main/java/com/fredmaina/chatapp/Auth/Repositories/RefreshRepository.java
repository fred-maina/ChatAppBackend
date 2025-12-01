package com.fredmaina.chatapp.Auth.Repositories;

import com.fredmaina.chatapp.Auth.Models.RefreshToken;
import com.fredmaina.chatapp.Auth.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUser(User user);
}
