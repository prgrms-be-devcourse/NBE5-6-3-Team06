package com.grepp.matnam.app.model.restaurant.repository;

import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RestaurantSuggestionRepositoryCustom {

    Page<RestaurantSuggestion> findAllSuggestion(Pageable pageable);

    Page<RestaurantSuggestion> findByStatusAndKeywordContaining(String status, String keyword, Pageable pageable);

    Page<RestaurantSuggestion> findByStatus(String status, Pageable pageable);

    Page<RestaurantSuggestion> findByKeywordContaining(String keyword, Pageable pageable);
}
