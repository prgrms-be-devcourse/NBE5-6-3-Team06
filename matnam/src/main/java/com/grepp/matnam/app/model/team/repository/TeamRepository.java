package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Team;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, TeamRepositoryCustom {

    List<Team> findTeamsByUser_UserIdAndActivatedTrue(String userId);

    Optional<Team> findByTeamIdAndActivatedTrue(Long teamId);

    long countAllByActivated(Boolean activated);

    long countByCreatedAtBetweenAndActivatedTrue(LocalDateTime localDateTime, LocalDateTime localDateTime1);

    long countByActivatedTrue();

    long countByStatusInAndActivatedTrue(List<Status> activeStatuses);

    long countByCreatedAtAfterAndActivatedTrue(LocalDateTime createdAtAfter);

    Long countByNowPeopleBetweenAndActivatedTrue(Integer nowPeopleAfter, Integer nowPeopleBefore);

    @Modifying
    @Query("UPDATE Team t SET t.viewCount = t.viewCount + :count WHERE t.teamId = :teamId")
    void increaseViewCountBy(@Param("teamId") Long teamId, @Param("count") Long count);
}