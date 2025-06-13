package com.grepp.matnam.app.model.mymap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoGeocodingService {

    private final RestaurantRepository restaurantRepository;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void updateAllRestaurantCoordinates() {
        List<Restaurant> restaurants = restaurantRepository.findAll();

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
                continue;
            }

            String address = restaurant.getAddress();
            Optional<double[]> latLng = getCoordinatesFromAddress(address);

            latLng.ifPresentOrElse(coords -> {
                restaurant.setLatitude(coords[0]);
                restaurant.setLongitude(coords[1]);
                restaurantRepository.saveAndFlush(restaurant);
                System.out.println("저장 완료: " + restaurant.getName());
            }, () -> {
                System.out.println("위경도 결과 없음: " + restaurant.getName() + " / 주소: " + address);
            });

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Optional<double[]> getCoordinatesFromAddress(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" + encodedAddress;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode documents = response.getBody().get("documents");

            if (documents.isArray() && documents.size() > 0) {
                JsonNode first = documents.get(0);
                double longitude = first.get("x").asDouble();
                double latitude = first.get("y").asDouble();
                return Optional.of(new double[]{latitude, longitude});
            }
        } catch (Exception e) {
            System.err.println("Geocoding error for: " + address);
            e.printStackTrace();
        }
        return Optional.empty();
    }
}