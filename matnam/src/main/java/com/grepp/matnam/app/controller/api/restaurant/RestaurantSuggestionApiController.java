package com.grepp.matnam.app.controller.api.restaurant;

import com.grepp.matnam.app.controller.api.restaurant.payload.RestaurantSuggestionRequest;
import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import com.grepp.matnam.app.model.restaurant.service.RestaurantSuggestionService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Void> createSuggestion(@RequestBody @Valid RestaurantSuggestionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = auth.getName();

        service.saveSuggestion(request, userId);
        return ResponseEntity.ok().build();
    }
}