package com.grepp.matnam.app.controller.api.restaurant;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RestaurantSuggestionModalController {

    @GetMapping("/modal/restaurant-suggestion")
    public String suggestionModal(){
        return "restaurants/restaurant-suggestion-modal";
    }
}