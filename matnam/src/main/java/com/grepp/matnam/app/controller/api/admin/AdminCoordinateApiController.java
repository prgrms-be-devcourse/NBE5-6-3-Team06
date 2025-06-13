package com.grepp.matnam.app.controller.api.admin;

import com.grepp.matnam.app.model.mymap.service.KakaoGeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/coordinate")
@RequiredArgsConstructor
public class AdminCoordinateApiController {

    private final KakaoGeocodingService kakaoGeocodingService;

    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> updateAllCoordinates() {
        kakaoGeocodingService.updateAllRestaurantCoordinates();
        Map<String, String> response = new HashMap<>();
        response.put("message", "좌표 업데이트가 완료되었습니다.");
        return ResponseEntity.ok(response);
    }
}