package com.grepp.matnam.app.controller.api.team;

import com.grepp.matnam.app.model.team.dto.FavoriteRequestDto;
import com.grepp.matnam.app.model.team.dto.TeamDto;
import com.grepp.matnam.app.model.team.service.FavoriteService;
import com.grepp.matnam.infra.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@Tag(name = "favorite API", description = "모임 즐겨찾기 관련 REST API")
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    //즐겨찾기 추가
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Void>> addFavorites(
        @Valid @RequestBody FavoriteRequestDto requestDto
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        favoriteService.addFavorite(userId, requestDto.getTeamId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // 즐겨찾기 해제
    @DeleteMapping("remove/{teamId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorites(
        @PathVariable Long teamId
    ) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        favoriteService.removeFavorite(userId, teamId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // 내 즐겨찾기 모임 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamDto>>> listFavorites() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TeamDto> favorite = favoriteService.getFavoriteForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(favorite));
    }

}
