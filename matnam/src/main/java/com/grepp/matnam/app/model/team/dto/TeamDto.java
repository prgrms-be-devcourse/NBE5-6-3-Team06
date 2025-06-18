package com.grepp.matnam.app.model.team.dto;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Team;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamDto {
    private Long teamId;
    private String teamTitle;
    private String teamDetails;
    private LocalDateTime teamDate;
    private String restaurantName;
    private String restaurantAddress;
    private Integer maxPeople;
    private Integer nowPeople;
    private String category;
    private String imageUrl;
    private Status status;
    private Long favoriteCount;
    private Double latitude;
    private Double longitude;
    private Long viewCount;

    public static TeamDto from(Team team) {
        TeamDto dto = new TeamDto();
        dto.setTeamId(team.getTeamId());
        dto.setTeamTitle(team.getTeamTitle());
        dto.setTeamDetails(team.getTeamDetails());
        dto.setTeamDate(team.getTeamDate());
        dto.setRestaurantName(team.getRestaurantName());
        dto.setRestaurantAddress(team.getRestaurantAddress());
        dto.setMaxPeople(team.getMaxPeople());
        dto.setNowPeople(team.getNowPeople());
        dto.setCategory(team.getCategory());
        dto.setImageUrl(team.getImageUrl());
        dto.setStatus(team.getStatus());
        dto.setFavoriteCount(team.getFavoriteCount());
        dto.setLatitude(team.getLatitude());
        dto.setLongitude(team.getLongitude());
        dto.setViewCount(team.getViewCount());
        return dto;
    }
}