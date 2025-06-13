package com.grepp.matnam.app.model.auth.token.service;

import com.grepp.matnam.app.model.auth.token.entity.RefreshToken;
import com.grepp.matnam.app.model.auth.token.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void deleteByAccessTokenId(String accessTokenId) {
        Optional<RefreshToken> optional = refreshTokenRepository.findByAccessTokenId(accessTokenId);
        optional.ifPresent(refreshToken -> refreshTokenRepository.deleteById(refreshToken.getId()));
    }

    public RefreshToken renewingToken(String accessTokenId, String newTokenId) {
        RefreshToken refreshToken = findByAccessTokenId(accessTokenId);
        if (refreshToken != null) {
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setAccessTokenId(newTokenId);
            return refreshTokenRepository.save(refreshToken);
        }
        return null;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token).orElse(null);
    }

    public RefreshToken findByAccessTokenId(String accessTokenId){
        return refreshTokenRepository.findByAccessTokenId(accessTokenId).orElse(null);
    }

    public RefreshToken save(RefreshToken refreshToken) {
        try {
            return refreshTokenRepository.save(refreshToken);
        } catch (Exception e) {
            log.error("RefreshToken 저장 실패 - userId: {}, 오류: {}", refreshToken.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    public void deleteByUserId(String userId) {
        log.info("사용자별 RefreshToken 삭제 시작 - userId: {}", userId);
        try {
            refreshTokenRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.error("사용자별 RefreshToken 삭제 실패 - userId: {}, 오류: {}", userId, e.getMessage(), e);
        }
    }
}