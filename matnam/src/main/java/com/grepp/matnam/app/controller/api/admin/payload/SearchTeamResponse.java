package com.grepp.matnam.app.controller.api.admin.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchTeamResponse {
    private String leaderId;
    private String teamTitle;
    private List<String> userIds;
}
