package com.grepp.matnam.app.model.kafka.service;

import com.grepp.matnam.app.controller.api.auth.payload.SignupRequest;
import com.grepp.matnam.app.model.kafka.dto.SmtpDto;
import com.grepp.matnam.app.model.user.entity.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendSignupEvent(String email, String emailCode, String nickName) {
        // SmtpDto 변환해서 전송
        SmtpDto smtpDto = SmtpDto.builder()
            .from("studyseok97@gmail.com")
            .subject("맛남 회원가입을 환영합니다!")
            .to(email)
            .eventType("USER_SIGNUP")
            .templatePath("/mail/signup-verification")
            .properties(Map.of(
                "nickname", nickName,
                "domain", "http://localhost:8080",
                "emailCode", emailCode
            ))
            .build();

        // kafkaTemplate : Kafka 로 메시지를 쉽게 보낼 수 있게 해주는 템플릿
        kafkaTemplate.send("user-signup", smtpDto);
    }
}
