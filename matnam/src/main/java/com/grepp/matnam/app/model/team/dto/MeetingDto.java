package com.grepp.matnam.app.model.team.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString
public class MeetingDto {
    private Long teamId;
    private String teamTitle;
    private Long participantId;
    private String userId;
}
