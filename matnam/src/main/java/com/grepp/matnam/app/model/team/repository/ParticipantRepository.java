package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.entity.Participant;
import com.grepp.matnam.app.model.team.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom  {

    Participant findByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    boolean existsByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    List<Participant> findByTeam_TeamId(Long teamId);

    List<Participant> findByUser_UserId(String userId);

    @Query("SELECT p FROM Participant p JOIN FETCH p.user WHERE p.team.teamId = :teamId")
    List<Participant> findParticipantsWithUserByTeamId(@Param("teamId") Long teamId);

    boolean existsByUser_UserIdAndTeam_TeamIdAndParticipantStatus(String userId, Long teamId, ParticipantStatus participantStatus);

    List<Participant> findByTeam_TeamIdAndParticipantStatus(Long teamId, ParticipantStatus participantStatus);
}
