package com.grepp.matnam.app.model.restaurant.service;

import com.grepp.matnam.app.model.restaurant.code.SuggestionStatus;
import com.grepp.matnam.app.model.restaurant.document.RestaurantEmbedding;
import com.grepp.matnam.app.model.restaurant.dto.RestaurantSuggestionDto;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import com.grepp.matnam.app.model.restaurant.repository.RestaurantSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.util.StringUtils;

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

    public Page<RestaurantSuggestion> findByFilter(String status, String keyword, Pageable pageable) {
        if (!status.isBlank() && StringUtils.hasText(keyword)) {
            return suggestionRepository.findByStatusAndKeywordContaining(status, keyword, pageable);
        } else if (StringUtils.hasText(status)) {
            return suggestionRepository.findByStatus(status, pageable);
        } else if (StringUtils.hasText(keyword)) {
            return suggestionRepository.findByKeywordContaining(keyword, pageable);
        } else {
            return suggestionRepository.findAllSuggestion(pageable);
        }
    }

    @Transactional
    public void unActivatedSuggestion(Long suggestionId) {
        RestaurantSuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new IllegalArgumentException("식당을 찾을 수 없습니다."));
        suggestion.setActivated(false);
    }
}