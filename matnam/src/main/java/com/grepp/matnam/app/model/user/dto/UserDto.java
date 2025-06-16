package com.grepp.matnam.app.model.user.dto;

import com.grepp.matnam.app.model.user.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String role;
    private String name;
    private String username;
    private String userId;
    private String email;
    private String address;
    private String provider;
    private String providerId;
    private int age;
    private String gender;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        dto.setAge(user.getAge());
        dto.setGender(user.getGender().toString());
        return dto;
    }
}