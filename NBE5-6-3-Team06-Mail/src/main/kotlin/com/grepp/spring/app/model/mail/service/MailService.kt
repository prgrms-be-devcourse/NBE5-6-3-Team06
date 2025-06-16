package com.grepp.spring.app.model.mail.service

import com.grepp.spring.infra.mail.MailTemplate
import com.grepp.spring.infra.mail.SmtpDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MailService(
    private val template: MailTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    val semaphore = Semaphore(3)

    fun send(dto: SmtpDto) = runBlocking {
        launch(Dispatchers.IO) {
            semaphore.withPermit {
                template.send(dto)
            }
        }
    }
}