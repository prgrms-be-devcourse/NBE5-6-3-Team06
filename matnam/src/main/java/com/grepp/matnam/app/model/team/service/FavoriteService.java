package com.grepp.matnam.app.model.team.service;

import com.grepp.matnam.app.model.team.dto.TeamDto;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.entity.Favorite;
import com.grepp.matnam.app.model.team.repository.FavoriteRepository;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final TeamService teamService;

    //즐겨찾기 추가
    @Transactional
    public void addFavorite(String userId, Long teamId) {
        if(favoriteRepository.existsByUser_UserIdAndTeam_TeamId(userId, teamId)) {
            throw new IllegalArgumentException("이미 즐겨찾기된 모임입니다");
        }
        User user = userService.getUserById(userId);
        Team team = teamService.getTeamById(teamId);
        favoriteRepository.save(new Favorite(user, team));
    }

    // 즐겨찾기 해제
    @Transactional
    public void removeFavorite(String userId, Long teamId) {
        Favorite fav = favoriteRepository.findByUser_UserIdAndTeam_TeamId(userId, teamId)
            .orElseThrow(() -> new EntityNotFoundException("즐겨찾기가 없습니다."));
        favoriteRepository.delete(fav);
    }

    // 내 즐겨찾기 모임 조회
    @Transactional(readOnly = true)
    public List<TeamDto> getFavoriteForUser(String userId) {
        return favoriteRepository.findAllByUser_UserId(userId)
            .stream()
            .map(f -> TeamDto.from(f.getTeam()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Object existsByUserAndTeam(String userId, Long teamId) {
        return favoriteRepository
            .existsByUser_UserIdAndTeam_TeamId(userId, teamId);
    }
}
