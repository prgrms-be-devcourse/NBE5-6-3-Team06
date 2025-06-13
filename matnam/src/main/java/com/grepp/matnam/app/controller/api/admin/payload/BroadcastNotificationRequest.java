package com.grepp.matnam.app.controller.api.admin.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

@Data
public class BroadcastNotificationRequest {
    @NotBlank
    private String content;
    @NotBlank
    private String targetType;
    @NotNull
    @Size(min = 1, message = "알림 대상 사용자는 1명 이상이어야 합니다.")
    private Set<String> targetUserIds;
}
