package com.grepp.matnam.app.model.auth.token.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.OffsetDateTime;

@Getter @Setter
@RedisHash("userBlackList")
public class UserBlackList {
    @Id
    private String userId;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public UserBlackList(String userId) {
        this.userId = userId;
    }
}