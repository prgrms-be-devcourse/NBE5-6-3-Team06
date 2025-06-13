package com.grepp.matnam.app.model.restaurant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantSuggestionDto {
    private String name;
    private String address;
    private String mainFood;
    private Double latitude;
    private Double longitude;
    private Long submittedByUserId;

    public RestaurantSuggestionDto() {
    }
}