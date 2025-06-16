package com.grepp.matnam.app.model.kafka.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmtpDto {
    private String from;
    private String subject;
    private String to;
    private Map<String, String> properties;
    private String templatePath;
    private String eventType;
}
