package com.grepp.spring.infra.mail

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("SmtpDto")
data class SmtpDto(
    val from:String,
    val subject:String,
    val to:String,
    val properties:Map<String, Any>? = mutableMapOf(),
    val templatePath:String,
    val eventType: String
)