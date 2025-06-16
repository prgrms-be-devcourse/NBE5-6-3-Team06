package com.grepp.matnam.app.controller.api.team.payload;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UpdatedTeamRequest {

    @NotBlank(message = "제목은 필수 입력 값입니다.")
    @Size(max = 20, message = "제목은 20자를 초과할 수 없습니다.")
    private String title;
    @NotBlank(message = "설명은 필수 입력 값입니다.")
    @Size(max = 300, message = "설명은 300자를 초과할 수 없습니다.")
    private String description;
    @NotBlank(message = "날짜는 필수 입력 값입니다.")
    private String date;
    @NotBlank(message = "시간은 필수 입력 값입니다.")
    private String time;
    private String category;
    private int maxPeople;
    @NotBlank(message = "식당 이름은 필수 입력 값입니다.")
    private String restaurantName;
    @NotBlank(message = "식당 주소는 필수 입력 값입니다.")
    private String restaurantAddress;
    @NotNull(message = "상호명 검색을 통해 정확한 주소를 입력해주세요.")
    private Double latitude;
    @NotNull(message = "상호명 검색을 통해 정확한 주소를 입력해주세요.")
    private Double longitude;
    private MultipartFile imageUrl;

    public static UpdatedTeamRequest fromEntity(Team team) {
        UpdatedTeamRequest dto = new UpdatedTeamRequest();
        dto.setTitle(           team.getTeamTitle());
        dto.setDescription(     team.getTeamDetails());
        dto.setCategory(        team.getCategory());
        dto.setMaxPeople(       team.getMaxPeople());
        dto.setRestaurantName(  team.getRestaurantName());
        dto.setRestaurantAddress(team.getRestaurantAddress());
        dto.setLatitude(        team.getLatitude());
        dto.setLongitude(        team.getLongitude());
        if (team.getTeamDate() != null) {
            LocalDateTime dt = team.getTeamDate();
            dto.setDate( dt.toLocalDate().toString() );
            dto.setTime( dt.toLocalTime().withSecond(0).toString() );
        }
        return dto;
    }

    public Team toTeam(String imageUrl) {
        Team team = new Team();
        team.setTeamTitle(this.title);
        team.setTeamDetails(this.description);
        team.setMaxPeople(this.maxPeople);
        team.setCategory(this.category);
        team.setRestaurantName(this.restaurantName);
        team.setRestaurantAddress(this.restaurantAddress);
        team.setLatitude(this.latitude);
        team.setLongitude(this.longitude);
        team.setImageUrl(imageUrl);

        if (this.date != null && this.time != null) {
            String dateTimeString = this.date + "T" + this.time + ":00";
            try {
                team.setTeamDate(LocalDateTime.parse(dateTimeString));
            } catch (Exception e) {
                team.setTeamDate(LocalDateTime.now());
            }
        } else {
            team.setTeamDate(LocalDateTime.now());
        }

        team.setNowPeople(1);
        team.setStatus(Status.RECRUITING);

        return team;
    }
}
