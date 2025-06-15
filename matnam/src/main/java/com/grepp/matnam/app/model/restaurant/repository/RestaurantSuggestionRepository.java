package com.grepp.matnam.app.model.restaurant.repository;

import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantSuggestionRepository extends
        JpaRepository<RestaurantSuggestion, Long>, RestaurantSuggestionRepositoryCustom {
}