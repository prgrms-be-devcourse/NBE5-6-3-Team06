package com.grepp.matnam.app.controller.api.restaurant;

import com.grepp.matnam.app.controller.api.restaurant.payload.RestaurantRecommendResponse;

import com.grepp.matnam.app.model.restaurant.service.RestaurantAiService;
import com.grepp.matnam.app.model.restaurant.service.RestaurantService;
import com.grepp.matnam.app.model.restaurant.dto.RestaurantDto;
import com.grepp.matnam.app.model.team.service.TeamService;
import com.grepp.matnam.infra.response.ApiResponse;
import com.grepp.matnam.infra.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class RestaurantApiController {

    private final RestaurantAiService restaurantAiService;
    private final TeamService teamService;
    private final RestaurantService restaurantService;

    // LLM 간단한 테스트 채팅 메시지[LLM 연결 확인용]
    @GetMapping("chat")
    @Operation(summary = "LLM 연결 테스트", description = "메세지를 통해 LLM 연결 테스트합니다.")
    public ResponseEntity<ApiResponse<String>> chat(String message) {
        try {
            return ResponseEntity.ok(ApiResponse.success(restaurantAiService.chat(message)));
        } catch (Exception e) {
            log.error("LLM 오류", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("recommend/restaurant/{teamId}")
    @Operation(summary = "추천", description = "팀에 속한 사용자의 취향 키워드, 거리를 종합하여 이를 기반으로 추천을 제공합니다.")
    public ResponseEntity<ApiResponse<RestaurantRecommendResponse>> recommend(@PathVariable Long teamId, @RequestParam String category) {
        try {
            if(teamId == null||teamId <= 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.BAD_REQUEST));
            }
            List<String> keywords = teamService.countPreferenceKeyword(teamId);
            log.info("teamId {}에 대한 추출된 키워드: {}", teamId, keywords);

            String address = teamService.getTeamByIdWithParticipants(teamId).getRestaurantAddress();
            log.info("일차장소: {}", address);
            log.info("팀 카테고리: {}", category);

            String keywordPrompt = "우리 팀은 다음 키워드를 선호해요: " + String.join(" ", keywords);

            if (category != null && !category.isEmpty() && !category.equals("카테고리")) {
                keywordPrompt += " 그리고 우리팀이 원하는 카테고리는 " + category + "입니다.";
            }
            keywordPrompt += " 현재 위치는 " + address + "일 때 키워드를 잘 반영하고";

            keywordPrompt += " 너무 먼 거리는 제외하고 카테고리는 무조건 동일한 최적의 식당 3곳을 추천해줘.";

            log.info("최종 프롬프트: {}", keywordPrompt);

            RestaurantRecommendResponse response = restaurantAiService.recommendRestaurant(keywordPrompt);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("추천 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // 재추천
    @GetMapping("reRecommend/restaurant/{teamId}")
    @Operation(summary = "재추천", description = "취향 기반 추천이 마음에 들지 않을 때 제공하는 재추천입니다.")
    public ResponseEntity<ApiResponse<RestaurantRecommendResponse>> reRecommend(@PathVariable Long teamId) {
        try {
            String address = teamService.getTeamByIdWithParticipants(teamId).getRestaurantAddress();

            String rePrompt = "현재 1차 장소는 " + address + "입니다. " +
                "이 위치에서 너무 멀지 않은 거리에 있는 " +
                "좋은 2차 식당 3곳을 추천해주세요. " +
                "각 식당의 특징과 추천 이유를 자세히 설명해주세요.";

            return ResponseEntity.ok(ApiResponse.success(restaurantAiService.reRecommendRestaurant(rePrompt)));
        } catch (Exception e) {
            log.error("재추천 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    //식당 정보 불러오기
    @GetMapping("/restaurant/name")
    @Operation(summary = "식당 정보 조회", description = "사용자에게 전달하기 위해 식당 정보 조회입니다.")
    public ResponseEntity<ApiResponse<RestaurantDto>> getRestaurantByName(@RequestParam String name) {
        try {
            if(name==null){
                log.warn("매개변수 name 문제");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.BAD_REQUEST));
            }

            RestaurantDto dto = restaurantService.findByName(name);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ResponseCode.NOT_FOUND));
            }
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터: {}", name);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(ResponseCode.BAD_REQUEST));
        } catch (Exception e) {
            log.error("식당 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }
}
