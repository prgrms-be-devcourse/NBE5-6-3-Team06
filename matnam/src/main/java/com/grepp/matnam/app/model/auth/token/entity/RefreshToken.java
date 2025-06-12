package com.grepp.matnam.app.model.auth.token.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.UUID;

@Getter @Setter
@RedisHash(value = "refreshToken", timeToLive = 3600 * 24 * 7)
public class RefreshToken {
    @Id
    private String id = UUID.randomUUID().toString();

    @Indexed
    private String userId;

    @Indexed
    private String accessTokenId;

    @Indexed
    private String token = UUID.randomUUID().toString();

    public RefreshToken(String userId, String accessTokenId) {
        this.userId = userId;
        this.accessTokenId = accessTokenId;
    }
}