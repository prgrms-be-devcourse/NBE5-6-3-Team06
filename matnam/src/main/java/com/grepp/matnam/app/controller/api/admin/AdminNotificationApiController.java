package com.grepp.matnam.app.controller.api.admin;

import com.grepp.matnam.app.controller.api.admin.payload.SearchTeamResponse;
import com.grepp.matnam.app.controller.api.admin.payload.SearchUserResponse;
import com.grepp.matnam.app.controller.api.admin.payload.BroadcastNotificationRequest;
import com.grepp.matnam.app.model.notification.service.NotificationService;
import com.grepp.matnam.app.model.team.service.TeamService;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notification")
@Slf4j
@Tag(name = "Admin Notification API", description = "관리자 - 알림 관리 API")
public class AdminNotificationApiController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final TeamService teamService;

    @PostMapping("/broadcast")
    @Operation(summary = "공지사항 발송", description = "알림 대상 사용자들에게 공지사항을 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> broadcastNotice(
        @RequestBody @Valid BroadcastNotificationRequest request) {
        userService.sendBroadcastNotification(request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "공지사항 비활성화", description = "특정 공지사항을 비활성화합니다.")
    public ResponseEntity<ApiResponse<Void>> unActivatedNotice(@PathVariable Long noticeId) {
        notificationService.unActivatedById(noticeId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/{noticeId}")
    @Operation(summary = "공지사항 재발송", description = "기존 공지사항을 알림 대상 사용자들에게 재발송합니다.")
    public ResponseEntity<ApiResponse<Void>> resendNotice(@PathVariable Long noticeId,
        @RequestBody @Valid BroadcastNotificationRequest request) {
        userService.resendBroadcastNotification(noticeId, request);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/team/{keyword}")
    @Operation(summary = "모임 찾기", description = "공지사항 대상에 추가할 모임을 검색합니다.")
    public ResponseEntity<ApiResponse<List<SearchTeamResponse>>> getParticipantByLeader(@PathVariable String keyword) {
        List<SearchTeamResponse> teams = teamService.getParticipantByLeader(keyword);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/user/{keyword}")
    @Operation(summary = "사용자 찾기", description = "공지사항 대상에 추가할 사용자를 검색합니다.")
    public ResponseEntity<ApiResponse<List<SearchUserResponse>>> getUserByUserId(@PathVariable String keyword) {
        List<SearchUserResponse> users = teamService.getUserByUserId(keyword);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

}
