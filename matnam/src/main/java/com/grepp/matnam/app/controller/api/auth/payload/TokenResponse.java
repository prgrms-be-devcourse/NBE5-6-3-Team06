package com.grepp.matnam.app.controller.api.auth.payload;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String grantType;
    private Long expiresIn;
}