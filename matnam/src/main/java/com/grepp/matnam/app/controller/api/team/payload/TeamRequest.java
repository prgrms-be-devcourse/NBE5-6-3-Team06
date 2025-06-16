package com.grepp.matnam.app.controller.api.team.payload;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.user.entity.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class TeamRequest {

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
    private Integer maxPeople;
    private Integer nowPeople;

    @NotBlank(message = "식당 이름은 필수 입력 값입니다.")
    private String restaurantName;

    @NotBlank(message = "식당 주소는 필수 입력 값입니다.")
    private String restaurantAddress;

    private MultipartFile imageUrl;

    @NotNull(message = "상호명 검색을 통해 정확한 주소를 입력해주세요.")
    private Double latitude;

    @NotNull(message = "상호명 검색을 통해 정확한 주소를 입력해주세요.")
    private Double longitude;

    public Team toEntity(User user, String imageUrl) {
        Team team = new Team();
        team.setUser(user);
        team.setTeamTitle(this.title);
        team.setTeamDetails(this.description);
        team.setMaxPeople(this.maxPeople);
        team.setCategory(this.category);
        team.setNowPeople(1);
        team.setStatus(Status.RECRUITING);
        team.setRestaurantName(this.restaurantName);
        team.setRestaurantAddress(this.restaurantAddress);
        team.setLatitude(this.latitude);
        team.setLongitude(this.longitude);
        team.setImageUrl(imageUrl);
        return team;
    }

}


