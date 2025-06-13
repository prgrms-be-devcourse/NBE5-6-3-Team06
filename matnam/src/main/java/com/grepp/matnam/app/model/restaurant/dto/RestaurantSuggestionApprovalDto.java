package com.grepp.matnam.app.model.restaurant.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantSuggestionApprovalDto {
    private String category;

    private String tel;

    private String openTime;

    private String summary;

    private String mainFood;

    private Float googleRating;

    private Double latitude;

    private Double longitude;

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
}   //이름, 주소, 위경도, 대표메뉴 말고는 관리자가 직접 채워서 저장해야함