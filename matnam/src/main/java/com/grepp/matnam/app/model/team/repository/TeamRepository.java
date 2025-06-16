package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Team;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    @Query("UPDATE Team t SET t.viewCount = t.viewCount + 1 WHERE t.teamId = :teamId")
    void increaseViewCount(@Param("teamId") Long teamId);
}