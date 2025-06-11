package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.ParticipantStatus;

public interface ParticipantRepositoryCustom {
    long countApprovedExcludingHost(Long teamId, ParticipantStatus status);

}
