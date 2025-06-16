package com.grepp.spring.infra.kafka

import com.grepp.spring.app.model.mail.service.MailService
import com.grepp.spring.infra.mail.SmtpDto
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SignupEventConsumer(
    private val mailService: MailService
){
    private val log = LoggerFactory.getLogger(javaClass)

    // todo 카프카를 통해 메시지큐에 메시지가 들어오면 얘가 자동적으로 실행
    @KafkaListener(topics = ["user-signup"], groupId = "mail-service")
    fun handleSignupEvent(smtpDto: SmtpDto) {
        log.info("회원가입 메일 발송 요청 수신: ${smtpDto.to}")

        try {
            // MailService 로 메일 발송
            mailService.send(smtpDto)
            log.info("회원가입 메일 발송 완료: ${smtpDto.to}")

        } catch (e: Exception) {
            log.error("회원가입 메일 발송 실패: ${smtpDto.to}, 오류: ${e.message}")
        }
    }
}
