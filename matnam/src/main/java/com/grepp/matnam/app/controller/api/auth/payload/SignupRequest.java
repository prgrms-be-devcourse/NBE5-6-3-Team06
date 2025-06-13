package com.grepp.matnam.app.controller.api.auth.payload;

import com.grepp.matnam.app.model.user.code.Gender;
import com.grepp.matnam.app.model.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "아이디는 필수입니다.")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    private String address;

    @Min(value = 10, message = "나이는 10세 이상이어야 합니다.")
    @Max(value = 100, message = "나이는 100세 이하여야 합니다.")
    private int age;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    public User toEntity() {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setAddress(address);
        user.setAge(age);
        user.setGender(gender);
        return user;
    }
}