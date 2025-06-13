package com.grepp.matnam.app.model.auth.token.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenDto {
    private String accessToken;
    private String refreshToken;
    private String grantType;
    private Long atExpiresIn;
    private Long rtExpiresIn;
}