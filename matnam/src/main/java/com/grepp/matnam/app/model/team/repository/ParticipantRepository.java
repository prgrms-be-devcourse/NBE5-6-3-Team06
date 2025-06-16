package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.entity.Participant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom  {

    Participant findByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    Optional<Participant> findByUser_UserIdAndTeam_TeamIdAndActivatedTrue(String userId, Long teamId);

    boolean existsByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    List<Participant> findByTeam_TeamId(Long teamId);

    List<Participant> findByUser_UserId(String userId);

    boolean existsByUser_UserIdAndTeam_TeamIdAndParticipantStatusAndActivatedTrue(String userId, Long teamId, ParticipantStatus participantStatus);

    boolean existsByUser_UserIdAndTeam_TeamIdAndParticipantStatus(String userId, Long teamId, ParticipantStatus participantStatus);

    List<Participant> findByTeam_TeamIdAndParticipantStatus(Long teamId, ParticipantStatus participantStatus);
}
