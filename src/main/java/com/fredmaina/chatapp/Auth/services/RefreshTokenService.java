package com.fredmaina.chatapp.Auth.services;

import com.fredmaina.chatapp.Auth.Models.RefreshToken;
import com.fredmaina.chatapp.Auth.Models.User;
import com.fredmaina.chatapp.Auth.Repositories.RefreshRepository;
import com.fredmaina.chatapp.Auth.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    private final RefreshRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshRepository repo, UserRepository userRepo) {
        this.refreshTokenRepository = repo;
        this.userRepository = userRepo;
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired");
        }
        return token;
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}


