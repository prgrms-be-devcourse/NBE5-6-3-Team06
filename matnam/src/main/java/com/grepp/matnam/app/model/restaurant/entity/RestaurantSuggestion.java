package com.grepp.matnam.app.model.restaurant.entity;

import com.grepp.matnam.app.model.restaurant.code.SuggestionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_suggestions")
@Getter
@Setter
public class RestaurantSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "main_food", nullable = false)
    private String mainFood;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "submitted_by_user_id")
    private String submittedByUserId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();
}