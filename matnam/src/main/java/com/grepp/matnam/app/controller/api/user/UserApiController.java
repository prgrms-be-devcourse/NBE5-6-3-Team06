package com.grepp.matnam.app.controller.api.user;

import com.grepp.matnam.app.controller.api.user.payload.PasswordChangeRequest;
import com.grepp.matnam.app.controller.api.user.payload.PreferenceRequest;
import com.grepp.matnam.app.controller.api.user.payload.UserDeactivateRequest;
import com.grepp.matnam.app.model.user.dto.UserDto;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.PreferenceService;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.response.ApiResponse;
import com.grepp.matnam.infra.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User API", description = "사용자 관리 API")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;
    private final PreferenceService preferenceService;

    @PostMapping("/preference")
    @Operation(summary = "취향 설정", description = "사용자의 취향을 설정합니다.")
    public ResponseEntity<ApiResponse<PreferenceRequest>> setPreference(@Validated @RequestBody PreferenceRequest request) {
        String currentUserId = AuthenticationUtils.getCurrentUserId();

        preferenceService.savePreference(currentUserId, request);

        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @PutMapping("/preference")
    @Operation(summary = "취향 변경", description = "사용자의 취향을 변경합니다.")
    public ResponseEntity<ApiResponse<PreferenceRequest>> updatePreference(@Validated @RequestBody PreferenceRequest request) {
        String currentUserId = AuthenticationUtils.getCurrentUserId();

        preferenceService.updatePreference(currentUserId, request);

        return ResponseEntity.ok(ApiResponse.success(request));
    }

    @DeleteMapping
    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 비활성화(탈퇴) 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(@Validated @RequestBody UserDeactivateRequest request) {
        String currentUserId = AuthenticationUtils.getCurrentUserId();

        userService.deactivateAccount(currentUserId, request.getPassword());

        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PutMapping("/password")
    @Operation(summary = "비밀번호 변경", description = "사용자의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Validated @RequestBody PasswordChangeRequest request) {
        String currentUserId = AuthenticationUtils.getCurrentUserId();

        userService.changePassword(currentUserId, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("{userId}/temperature")
    @Operation(summary = "사용자 매너온도 조회", description = "특정 사용자의 매너 온도를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserTemperature(@PathVariable String userId) {
        User user = userService.getUserById(userId);

        Map<String, Object> temperature = new HashMap<>();

        temperature.put("userId", user.getUserId());
        temperature.put("temperature", user.getTemperature());

        return ResponseEntity.ok(ApiResponse.success(temperature));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUserInfo() {
        String currentUserId = AuthenticationUtils.getCurrentUserId();
        User user = userService.getUserById(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(UserDto.from(user)));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateUserInfo(@RequestBody UserDto userDto) {
        String currentUserId = AuthenticationUtils.getCurrentUserId();
        userService.updateUser(currentUserId, userDto);
        return ResponseEntity.ok(ApiResponse.success("회원 정보가 업데이트되었습니다."));
    }

}