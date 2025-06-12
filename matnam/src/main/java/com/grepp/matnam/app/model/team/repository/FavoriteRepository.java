package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    Optional<Favorite> findByUser_UserIdAndTeam_TeamId(String userId, Long teamId);

    List<Favorite> findAllByUser_UserId(String userId);
}
