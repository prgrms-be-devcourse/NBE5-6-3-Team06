package com.grepp.matnam.app.controller.api.restaurant.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantSuggestionRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String address;
    @NotBlank
    private String mainFood;
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
    @NotBlank
    private String submittedByUserId;
}
