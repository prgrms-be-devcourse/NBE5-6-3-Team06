package com.grepp.matnam.app.controller.api.restaurant;

import com.grepp.matnam.app.model.restaurant.dto.RestaurantSuggestionDto;
import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import com.grepp.matnam.app.model.restaurant.service.RestaurantSuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
public class RestaurantSuggestionApiController {

    private final RestaurantSuggestionService service;

    public RestaurantSuggestionApiController(RestaurantSuggestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<RestaurantSuggestion> getSuggestions() {
        return service.getAllSuggestions();
    }

    @PostMapping
    public ResponseEntity<Void> createSuggestion(@RequestBody RestaurantSuggestionDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(auth.getName());

        service.saveSuggestion(dto, userId);
        return ResponseEntity.ok().build();
    }
}