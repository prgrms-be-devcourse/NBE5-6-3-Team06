package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long>{

    boolean existsByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    Optional<Favorite> findByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    List<Favorite> findAllByUser_UserIdAndTeam_ActivatedTrueAndTeam_StatusNot(
        String userId,
        Status excludedStatus
    );

    // 팀ID 로 즐겨찾기 수 조회
    long countByTeam_TeamId(Long teamId);

}
