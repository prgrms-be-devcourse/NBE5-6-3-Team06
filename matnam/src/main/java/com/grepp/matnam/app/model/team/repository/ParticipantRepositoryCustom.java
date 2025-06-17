package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.entity.Participant;
import java.util.List;

public interface ParticipantRepositoryCustom {
    long countApprovedExcludingHost(Long teamId, ParticipantStatus status);

    List<Participant> findParticipantsWithUserByTeamId(Long teamId);
}
