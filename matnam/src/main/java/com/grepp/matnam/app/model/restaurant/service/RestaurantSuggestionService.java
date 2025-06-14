package com.grepp.matnam.app.model.restaurant.service;

import com.grepp.matnam.app.model.restaurant.code.SuggestionStatus;
import com.grepp.matnam.app.model.restaurant.dto.RestaurantSuggestionDto;
import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import com.grepp.matnam.app.model.restaurant.repository.RestaurantSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantSuggestionService {

    private final RestaurantSuggestionRepository suggestionRepository;

    public List<RestaurantSuggestion> getAllSuggestions() {
        return suggestionRepository.findAll();
    }

    public RestaurantSuggestion getById(Long id) {
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("식당이 존재하지 않습니다."));
    }

    @Transactional
    public void approveSuggestion(Long suggestionId) {
        RestaurantSuggestion suggestion = getById(suggestionId);
        suggestion.setStatus(SuggestionStatus.APPROVED);
    }

    @Transactional
    public void rejectSuggestion(Long suggestionId) {
        RestaurantSuggestion suggestion = getById(suggestionId);
        suggestion.setStatus(SuggestionStatus.REJECTED);
    }

    @Transactional
    public void createSuggestion(RestaurantSuggestion suggestion) {
        suggestionRepository.save(suggestion);
    }

    @Transactional
    public void saveSuggestion(RestaurantSuggestionDto dto, String userId) {
        RestaurantSuggestion suggestion = new RestaurantSuggestion();
        suggestion.setName(dto.getName());
        suggestion.setAddress(dto.getAddress());
        suggestion.setMainFood(dto.getMainFood());
        suggestion.setLatitude(dto.getLatitude());
        suggestion.setLongitude(dto.getLongitude());
        suggestion.setSubmittedByUserId(userId);
        suggestion.setStatus(SuggestionStatus.PENDING);
        suggestion.setSubmittedAt(LocalDateTime.now());

        suggestionRepository.save(suggestion);
    }
}