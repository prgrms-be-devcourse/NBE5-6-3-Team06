package com.grepp.matnam.app.model.restaurant.entity;

import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Restaurant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;

    private String name;

    private String summary;

    private String address;

    private String openTime;

    private String tel;

    private String mainFood;

    private String category;

    private Integer recommendedCount;

    private String mood;

    private Float googleRating;

    private boolean goodTalk;
    private boolean manyDrink;
    private boolean goodMusic;
    private boolean clean;
    private boolean goodView;
    private boolean isTerrace;
    private boolean goodPicture;
    private boolean goodMenu;
    private boolean longStay;
    private boolean bigStore;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Override
    public String toString() {
        return "Restaurant{" +
                "name='" + name + '\'' +
                ", summary='" + summary + '\'' +
                ", mainFood='" + mainFood + '\'' +
                ", category='" + category + '\'' +
                ", mood='" + mood + '\'' +
                '}';
    }
}
