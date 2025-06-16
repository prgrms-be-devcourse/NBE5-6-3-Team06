package com.grepp.matnam.app.model.team.service;

import com.grepp.matnam.app.controller.api.admin.payload.SearchTeamResponse;
import com.grepp.matnam.app.controller.api.admin.payload.SearchUserResponse;
import com.grepp.matnam.app.controller.api.admin.payload.StatDoubleResponse;
import com.grepp.matnam.app.controller.api.admin.payload.TeamStatusUpdateRequest;
import com.grepp.matnam.app.controller.web.admin.payload.ActiveTeamResponse;
import com.grepp.matnam.app.controller.web.admin.payload.NewTeamResponse;
import com.grepp.matnam.app.controller.web.admin.payload.TeamStatsResponse;
import com.grepp.matnam.app.facade.NotificationSender;
import com.grepp.matnam.app.model.chat.entity.ChatRoom;
import com.grepp.matnam.app.model.chat.repository.ChatRoomRepository;
import com.grepp.matnam.app.model.mymap.entity.Mymap;
import com.grepp.matnam.app.model.mymap.repository.MymapRepository;
import com.grepp.matnam.app.model.notification.code.NotificationType;
import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.code.Role;
import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.dto.MonthlyMeetingStatsDto;
import com.grepp.matnam.app.model.team.dto.ParticipantWithUserIdDto;
import com.grepp.matnam.app.model.team.entity.Participant;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.repository.ParticipantRepository;
import com.grepp.matnam.app.model.team.repository.TeamRepository;
import com.grepp.matnam.app.model.user.entity.Preference;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.repository.PreferenceRepository;
import com.grepp.matnam.app.model.user.repository.UserRepository;
import com.grepp.matnam.infra.error.exceptions.CommonException;
import com.grepp.matnam.infra.response.ResponseCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;

    private final ParticipantRepository participantRepository;
    private final PreferenceRepository preferenceRepository;
    private final MymapRepository mymapRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    private final NotificationSender notificationSender;

    private final RedisTemplate<String, String> redisTemplate;

    //게시글 조회수
    @Transactional
    public void increaseViewCount(Long teamId) {
        teamRepository.increaseViewCount(teamId);
    }

    @Transactional
    public void increaseViewCountIfNotViewedRecently(Long teamId, Long userId) {
        String redisKey = "viewed:team:" + teamId + ":user:" + userId;

        Boolean alreadyViewed = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(alreadyViewed)) {
            return;
        }
        teamRepository.increaseViewCount(teamId);

        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(1));
    }

    public void increaseViewCountWithUserOrIp(Long teamId, Long userId, HttpServletRequest request) {
        String redisKey;
        if (userId != null) {
            redisKey = "viewed:team:" + teamId + ":user:" + userId;
        } else {
            String clientIp = getClientIp(request);
            redisKey = "viewed:team:" + teamId + ":ip:" + clientIp;
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            return;
        }

        teamRepository.increaseViewCount(teamId);
        redisTemplate.opsForValue().set(redisKey, "1", Duration.ofHours(1));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // 모임 생성
    public void saveTeam(Team team) {
        teamRepository.save(team);
        // 1. ChatRoom 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setTeam(team);         // ChatRoom → Team 연결
        chatRoom.setName(team.getTeamId() + "번 채팅방");
        chatRoomRepository.save(chatRoom);

    }

    // 참여자 추가
    @Transactional
    public void addParticipant(Long teamId, User user) {
        Team team = teamRepository.findByTeamIdAndActivatedTrue(teamId)
            .orElseThrow(() -> new IllegalArgumentException("해당 모임이 존재하지 않습니다."));

        Optional<Participant> existing = Optional.ofNullable(
            participantRepository.findByUser_UserIdAndTeam_TeamId(user.getUserId(), teamId));

        if (existing.isPresent()) {
            Participant participant = existing.get();

            if (!participant.isActivated() || participant.getParticipantStatus() == ParticipantStatus.REJECTED) {
                // 기존 참여자가 나갔던 상태 → 다시 활성화해서 재신청
                participant.setParticipantStatus(ParticipantStatus.PENDING);
                participant.setActivated(true);
                participantRepository.save(participant);

                if (!user.getUserId().equals(team.getUser().getUserId())) {
                    sendNotification(team);
                }
                return;
            }
            throw new IllegalStateException("이미 참여한 사용자입니다.");
        }
        Participant participant = new Participant();
        participant.setTeam(team);
        participant.setUser(user);
        participant.setActivated(true);

        if (user.getUserId().equals(team.getUser().getUserId())) {
            participant.setParticipantStatus(ParticipantStatus.APPROVED);
            participant.setRole(Role.LEADER);

            if (team.getNowPeople() == null || team.getNowPeople() == 0) {
                team.setNowPeople(1);
            }
        } else {
            participant.setParticipantStatus(ParticipantStatus.PENDING);
            participant.setRole(Role.MEMBER);
        }
        participantRepository.save(participant);

        if (!user.getUserId().equals(team.getUser().getUserId())) {
            sendNotification(team);
        }
    }
    private void sendNotification(Team team) {
        notificationSender.sendNotificationToUser(
            team.getUser().getUserId(),
            NotificationType.TEAM_STATUS,
            "[" + team.getTeamTitle() + "] 모임에 참여 신청이 들어왔습니다!",
            "/team/detail/" + team.getTeamId()
        );
    }

    // 모임 참여 수락
    @Transactional
    public void approveParticipant(Long participantId, String userId) {
        Participant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참가자를 찾을 수 없습니다."));

        if (participant.getParticipantStatus() == ParticipantStatus.APPROVED) {
            throw new RuntimeException("이미 수락된 참가자입니다.");
        }

        Team team = participant.getTeam();
        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("모임 생성자만 참가 신청을 승인할 수 있습니다.");
        }

        if (team.getMaxPeople() != null && team.getNowPeople() >= team.getMaxPeople()) {
            throw new RuntimeException("모임의 최대 인원 수를 초과했습니다.");
        }

        participant.setParticipantStatus(ParticipantStatus.APPROVED);
        participantRepository.save(participant);

        if (team.getNowPeople() == null) {
            team.setNowPeople(1);
        } else {
            team.setNowPeople(team.getNowPeople() + 1);
        }
        updateTeamStatus(team);

        notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
            NotificationType.PARTICIPANT_STATUS, "[" + team.getTeamTitle() + "] 모임에 승인되었습니다!",
            "/team/detail/" + team.getTeamId());

        teamRepository.save(team);
    }

    @Transactional
    public int approveSelectedParticipants(String userId, Long teamId, List<Long> participantIds) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("모임이 존재하지 않습니다."));

        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("방장만 수락할 수 있습니다.");
        }

        int nowPeople = team.getNowPeople();
        int maxPeople = team.getMaxPeople();

        int availableSpots = maxPeople - nowPeople;
        int requestCount = participantIds.size();

        if (requestCount > availableSpots) {
            log.info("🔥 초과 인원 발생 - 예외 던지기 직전");
            throw new CommonException(ResponseCode.BAD_REQUEST,
                (requestCount - availableSpots) + "명이 초과되었습니다. 다시 선택해주세요.");
        }

        int approvedCount = 0;

        for (Long participantId : participantIds) {
            if (nowPeople >= maxPeople) {
                break;
            }
            Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new EntityNotFoundException("참여자를 찾을 수 없습니다."));

            if (participant.getParticipantStatus() == ParticipantStatus.PENDING) {
                participant.setParticipantStatus(ParticipantStatus.APPROVED);
                participant.setRole(Role.MEMBER);
                approvedCount++;
                nowPeople++;
            }
        }
        team.setNowPeople(nowPeople);
        updateTeamStatus(team);
        teamRepository.save(team);
        return approvedCount;
    }

    // 모임 참여 거절
    @Transactional
    public void rejectParticipant(Long participantId, String userId) {
        Participant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참가자를 찾을 수 없습니다."));

        if (participant.getParticipantStatus() == ParticipantStatus.PENDING) {
            Team team = participant.getTeam();
            if (!team.getUser().getUserId().equals(userId)) {
                throw new AccessDeniedException("모임 생성자만 참가 신청을 거절할 수 있습니다.");
            }
            participant.setParticipantStatus(ParticipantStatus.REJECTED);
            participantRepository.save(participant);
            notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
                NotificationType.PARTICIPANT_STATUS, "[" + team.getTeamTitle() + "] 모임에 거절되었습니다.",
                "/team/detail/" + team.getTeamId());
        } else {
            throw new IllegalStateException("대기 중인 참여자만 거절 가능합니다.");
        }
    }

    // 모임 업데이트
    @Transactional
    public void updateTeam(Long teamId, Team updatedTeam, String userId) {
        Team team = teamRepository.findByTeamIdAndActivatedTrue(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));

        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("모임 생성자만 수정할 수 있습니다.");
        }

        team.setTeamTitle(updatedTeam.getTeamTitle());
        team.setTeamDetails(updatedTeam.getTeamDetails());
        team.setRestaurantName(updatedTeam.getRestaurantName());
        team.setTeamDate(updatedTeam.getTeamDate());
        team.setMaxPeople(updatedTeam.getMaxPeople());
        team.setImageUrl(updatedTeam.getImageUrl());
        team.setNowPeople(updatedTeam.getNowPeople());
        team.setStatus(updatedTeam.getStatus());
        team.setRestaurantAddress(updatedTeam.getRestaurantAddress());
        team.setLatitude(updatedTeam.getLatitude());
        team.setLongitude(updatedTeam.getLongitude());
        team.setCategory(updatedTeam.getCategory());

        teamRepository.save(team);
    }

    // 모임 상태 변경 - 모임 취소
    @Transactional
    public void cancelTeam(Long teamId, String userId) {
        Team team = teamRepository.findByTeamIdAndActivatedTrue(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));

        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("모임 생성자만 취소할 수 있습니다.");
        }

        if (team.getStatus() == Status.COMPLETED) {
            throw new IllegalStateException("모임완료 상태에서는 취소할 수 없습니다.");
        }
        team.setStatus(Status.CANCELED);

        teamRepository.save(team);

        List<Participant> participants = team.getParticipants();
        for (Participant participant : participants) {
            if (participant.getParticipantStatus() == ParticipantStatus.APPROVED) {
                notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
                    NotificationType.TEAM_STATUS, "[" + team.getTeamTitle() + "] 모임이 취소되었습니다.",
                    null);
            }
        }
    }

    //조회 부분
    // 주최자로서의 팀 조회
    public List<Team> getTeamsByLeader(String userId) {
        return teamRepository.findTeamsByUser_UserIdAndActivatedTrue(userId);
    }

    //참여자로서의 팀 조회 (APPROVED 상태)
    public List<Team> getTeamsByParticipant(String userId) {
        return teamRepository.findTeamsByParticipantUserIdAndParticipantStatusAndActivatedTrue(
            userId, ParticipantStatus.APPROVED
        );
    }

    public List<Team> getAllTeams(String userId) {
        return teamRepository.findTeamsByParticipantUserIdAndParticipantStatusAndActivatedTrue(
            userId, ParticipantStatus.APPROVED);
    }

    // 사용자의 모든 참여 정보 조회 (PENDING, APPROVED, REJECTED)
    public List<Participant> getAllParticipantsForUser(String userId) {
        return participantRepository.findByUser_UserId(userId);
    }

    // 사용자가 참여한 모든 모임 조회(비활성화 제외)
    public List<Team> getAllTeamsForUser(String userId) {
        List<Participant> participants = getAllParticipantsForUser(userId);
        return participants.stream()
            .map(Participant::getTeam)
            .filter(Team::isActivated)
            .distinct()
            .collect(Collectors.toList());
    }

    // 참여자 상세 정보 조회(참여 상태)
    public Team getTeamById(Long teamId) {
        return teamRepository.findByTeamIdAndActivatedTrue(teamId).orElse(null);
    }

    // 모임 검색 페이지
    public Page<Team> getAllTeams(Pageable pageable, boolean includeCompleted) {
        return teamRepository.findAllWithParticipantsAndActivatedTrue(pageable, includeCompleted);
    }

    // 모임 즐겨찾기 카운트
    public Page<Team> getAllTeamsByFavoriteCount(Pageable pageable, boolean includeCompleted) {
        return teamRepository.findAllOrderByFavoriteCount(pageable, includeCompleted);
    }

    public Page<Team> getAllTeamsByViewCount(Pageable pageable, boolean includeCompleted) {
        return teamRepository.findAllOrderByViewCount(pageable, includeCompleted);
    }

    // 모임 상세 조회, 팀 페이지 조회
    @Transactional
    public Team getTeamByIdWithParticipants(Long teamId) {
        return teamRepository.findByIdWithParticipantsAndUserAndActivatedTrue(teamId).orElse(null);
    }
    // 모임 상태 변경 - 모임 완료
    @Transactional
    public void completeTeam(Long teamId, Status status, String userId) {
        log.info("팀 ID: {} 상태 변경 시도, 변경할 상태: {}", teamId, status);
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));

        log.info("현재 상태: {}", team.getStatus());
        Status prevStatus = team.getStatus();

        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("모임 생성자만 완료 처리할 수 있습니다.");
        }
        boolean hasMemberRole = team.getParticipants().stream()
            .anyMatch(participant -> participant.getRole() == Role.MEMBER);

        if (!hasMemberRole) {
            throw new IllegalStateException("참여자가 없는 모임은 완료 처리할 수 없습니다.");
        }
        team.setStatus(status);

        teamRepository.save(team);
        log.info("상태 변경 후: {}", team.getStatus());

        // 모임이 완료 상태가 되면 참여자들의 매너온도 증가
        if (status == Status.COMPLETED && prevStatus != Status.COMPLETED) {
            increaseTemperatureForCompletedTeam(team);
        }
        log.info("팀 상태 변경 완료: {}", team.getStatus());
        List<Participant> participants = team.getParticipants();
        for (Participant participant : participants) {
            if (participant.getParticipantStatus() == ParticipantStatus.APPROVED) {
                notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
                    NotificationType.REVIEW_REQUEST,
                    "[" + team.getTeamTitle() + "] 모임의 리뷰를 작성해주세요!",
                    "/team/" + team.getTeamId() + "/reviews");
                notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
                    NotificationType.TEAM_STATUS, "[" + team.getTeamTitle() + "] 모임이 완료되었습니다!",
                    null);
            }
        }
    }

    // 모임 나가기
    @Transactional
    public void leaveTeam(String userId, Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀이 존재하지 않습니다."));
        if (team.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("모임 주최자는 나갈 수 없습니다.");
        }

        Participant participant = participantRepository
            .findByUser_UserIdAndTeam_TeamIdAndActivatedTrue(userId, teamId)
            .orElseThrow(() -> new IllegalStateException("참가 신청 내역이 없거나 이미 탈퇴한 상태입니다."));

        if (participant.getParticipantStatus() != ParticipantStatus.APPROVED) {
            throw new IllegalStateException("수락된 상태에서만 나갈 수 있습니다");
        }
        participant.setActivated(false);

        team.setNowPeople(Math.max(0, team.getNowPeople() - 1));

        updateTeamStatus(team);
        teamRepository.save(team);
    }

    @Transactional
    public void kickParticipant(Long participantId, String userId) {
        Participant participant = participantRepository.findById(participantId)
            .orElseThrow(() -> new EntityNotFoundException("참가자를 찾을 수 없습니다."));
        Team team = participant.getTeam();

        if (!team.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("방장만 강제 탈퇴시킬 수 있습니다.");
        }
        if (!participant.isActivated()) {
            throw new IllegalStateException("이미 탈퇴된 참가자입니다.");
        }
        participant.setParticipantStatus(ParticipantStatus.REJECTED);
        participant.setActivated(false);

        team.setNowPeople(team.getNowPeople() - 1);
        updateTeamStatus(team);
        teamRepository.save(team);
    }


    // 상태 자동 관리
    @Transactional
    public void updateTeamStatus(Team team) {
        int now = team.getNowPeople() == null ? 0 : team.getNowPeople();
        int max = team.getMaxPeople() == null ? Integer.MAX_VALUE : team.getMaxPeople();

        if (now >= max && team.getStatus() != Status.FULL) {
            team.setStatus(Status.FULL);
        } else if (now < max && team.getStatus() == Status.FULL) {
            team.setStatus(Status.RECRUITING);
        }
    }

    private void increaseTemperatureForCompletedTeam(Team team) {
        List<Participant> participants = participantRepository.findByTeam_TeamId(team.getTeamId());

        for (Participant participant : participants) {
            if (participant.getParticipantStatus() == ParticipantStatus.APPROVED) {
                User user = participant.getUser();
                float currentTemp = user.getTemperature();
                float newTemp = currentTemp + 0.1f;

                user.setTemperature(newTemp);
                userRepository.save(user);
            }
        }
    }

    // 승인된 상태의 참여자 수
    public long getParticipantCountExcludingHost(Long teamId) {
        return participantRepository.countApprovedExcludingHost(teamId, ParticipantStatus.APPROVED);
    }

    public Page<Team> findByFilter(String status, String keyword, Pageable pageable) {
        if (!status.isBlank() && StringUtils.hasText(keyword)) {
            return teamRepository.findByStatusAndKeywordContaining(status, keyword, pageable);
        } else if (!status.isBlank()) {
            return teamRepository.findByStatus(status, pageable);
        } else if (StringUtils.hasText(keyword)) {
            return teamRepository.findByKeywordContaining(keyword, pageable);
        } else {
            return teamRepository.findAllUsers(pageable);
        }
    }

    public List<Participant> findAllWithUserByTeamId(Long teamId) {
        return participantRepository.findParticipantsWithUserByTeamId(teamId);
    }

    @Transactional
    public void updateTeamStatus(Long teamId, TeamStatusUpdateRequest teamStatusUpdateRequest) {
        Team team = teamRepository.findByTeamIdAndActivatedTrue(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));
        team.setStatus(teamStatusUpdateRequest.getStatus());

        List<ParticipantWithUserIdDto> participants = teamRepository.findAllDtoByTeamId(teamId);
        for (ParticipantWithUserIdDto dto : participants) {
            if (!teamStatusUpdateRequest.getReason().isBlank()) {
                notificationSender.sendNotificationToUser(dto.getUserId(),
                    NotificationType.TEAM_STATUS,
                    "[" + team.getTeamTitle() + "] 상태 변경 사유 : "
                        + teamStatusUpdateRequest.getReason(), null);
            }
            notificationSender.sendNotificationToUser(dto.getUserId(), NotificationType.TEAM_STATUS,
                "관리자에 의해 [" + team.getTeamTitle() + "] 모임의 상태가 [" + team.getStatus().getKoreanName()
                    + "](으)로 변경되었습니다.", "/team/page/" + teamId);
        }

    }

    @Transactional
    public void unActivatedById(Long teamId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new EntityNotFoundException("팀을 찾을 수 없습니다."));
        log.info("team {}", team);
        team.unActivated();
        List<ParticipantWithUserIdDto> participants = teamRepository.findAllDtoByTeamId(teamId);
        for (ParticipantWithUserIdDto dto : participants) {
            notificationSender.sendNotificationToUser(dto.getUserId(), NotificationType.TEAM_STATUS,
                "관리자에 의해  [" + team.getTeamTitle() + "] 모임이 삭제되었습니다.", null);
        }
    }

    public NewTeamResponse getNewTeamStats() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        long newTeams = teamRepository.countByCreatedAtBetweenAndActivatedTrue(today.atStartOfDay(),
            today.plusDays(1).atStartOfDay());
        long totalTeamCount = teamRepository.countByActivatedTrue();
        long yesterdayTotalTeamCount = totalTeamCount - newTeams;
        String teamGrowth = calculateGrowthRate(totalTeamCount, yesterdayTotalTeamCount);
        return new NewTeamResponse(newTeams, teamGrowth);
    }

    public ActiveTeamResponse getActiveTeamStats() {

        List<Status> activeStatuses = List.of(Status.RECRUITING, Status.FULL);
        long todayActiveTeams = teamRepository.countByStatusInAndActivatedTrue(activeStatuses);
        long totalTeamCount = teamRepository.countAllByActivated(true);

        return new ActiveTeamResponse(todayActiveTeams, totalTeamCount);
    }

    private String calculateGrowthRate(long today, long yesterday) {
        if (yesterday == 0) {
            if (today == 0) {
                return "+0%";
            }
            return "+100%"; // 또는 "N/A"
        }
        long diff = today - yesterday;
        double percent = ((double) diff / yesterday) * 100;
        String sign = percent >= 0 ? "+" : "";
        return String.format("%s%.0f%%", sign, percent);
    }

    public List<StatDoubleResponse> getMonthlyMeetingSuccessRate() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6).withDayOfMonth(1)
            .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<MonthlyMeetingStatsDto> monthlyStats = teamRepository.findMonthlyMeetingStats(
            sixMonthsAgo);
        return monthlyStats.stream().map(stat -> {
            String month = stat.getMeetingMonth();
            Long total = stat.getTotalMeetings();
            Long completed = stat.getCompletedMeetings();

            double successRate =
                (total != null && total > 0) ? ((double) completed / total) * 100 : 0.0;

            return new StatDoubleResponse(month, Math.round(successRate * 100.0) / 100.0);
        }).collect(Collectors.toList());
    }

    // 특정 팀의 사용자 teamId를 통해 userId 리스트 받기[상태가 승인인 사용자]
    public List<Participant> getApprovedUserIdsByTeamId(Long teamId) {
        return participantRepository.findByTeam_TeamIdAndParticipantStatus(teamId,
            ParticipantStatus.APPROVED);
    }

    //팀원들의 취향 키워드 종합
    public List<String> countPreferenceKeyword(Long teamId) {
        //집계 시작
        List<Participant> approvedParticipants = getApprovedUserIdsByTeamId(teamId);

        if (approvedParticipants.isEmpty()) {
            throw new RuntimeException("사용자가 없습니다.");
        } else {
            //집계 값을 받을 Map
            Map<String, Integer> keywordCount = new HashMap<>();

            keywordCount.put("goodTalk", 0);
            keywordCount.put("manyDrink", 0);
            keywordCount.put("goodMusic", 0);
            keywordCount.put("clean", 0);
            keywordCount.put("goodView", 0);
            keywordCount.put("isTerrace", 0);
            keywordCount.put("goodPicture", 0);
            keywordCount.put("goodMenu", 0);
            keywordCount.put("longStay", 0);
            keywordCount.put("bigStore", 0);

            //집계 핵심
            for (Participant participant : approvedParticipants) {
                User user = participant.getUser();
                Preference pref = preferenceRepository.findByUser(user);

                if (pref.isGoodTalk()) {
                    keywordCount.put("goodTalk", keywordCount.get("goodTalk") + 1);
                }
                if (pref.isManyDrink()) {
                    keywordCount.put("manyDrink", keywordCount.get("manyDrink") + 1);
                }
                if (pref.isGoodMusic()) {
                    keywordCount.put("goodMusic", keywordCount.get("goodMusic") + 1);
                }
                if (pref.isClean()) {
                    keywordCount.put("clean", keywordCount.get("clean") + 1);
                }
                if (pref.isGoodView()) {
                    keywordCount.put("goodView", keywordCount.get("goodView") + 1);
                }
                if (pref.isTerrace()) {
                    keywordCount.put("isTerrace", keywordCount.get("isTerrace") + 1);
                }
                if (pref.isGoodPicture()) {
                    keywordCount.put("goodPicture", keywordCount.get("goodPicture") + 1);
                }
                if (pref.isGoodMenu()) {
                    keywordCount.put("goodMenu", keywordCount.get("goodMenu") + 1);
                }
                if (pref.isLongStay()) {
                    keywordCount.put("longStay", keywordCount.get("longStay") + 1);
                }
                if (pref.isBigStore()) {
                    keywordCount.put("bigStore", keywordCount.get("bigStore") + 1);
                }

            }
            // 최대값 찾기
            int max = keywordCount.values().stream().max(Integer::compareTo).orElse(0);

            // 최댓값 키워드
            List<String> topKeywords = keywordCount.entrySet().stream().
                filter(entry -> entry.getValue() == max).
                map(Map.Entry::getKey).
                toList();

            return topKeywords;

        }
    }

    public Map<String, Long> getTeamParticipantDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("2-3명", teamRepository.countByNowPeopleBetweenAndActivatedTrue(2, 3));
        distribution.put("4-5명", teamRepository.countByNowPeopleBetweenAndActivatedTrue(4, 5));
        distribution.put("6-7명", teamRepository.countByNowPeopleBetweenAndActivatedTrue(6, 7));
        distribution.put("8-10명", teamRepository.countByNowPeopleBetweenAndActivatedTrue(8, 10));
        return distribution;
    }

    public Map<String, Long> getDailyNewTeamCountsLast7Days() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> dailyCounts = IntStream.iterate(6, i -> i - 1).limit(7)
            .mapToObj(today::minusDays)
            .collect(Collectors.toMap(
                date -> date,
                date -> teamRepository.countByCreatedAtBetweenAndActivatedTrue(
                    LocalDateTime.of(date, LocalTime.MIN),
                    LocalDateTime.of(date, LocalTime.MAX)
                ),
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
            ));

        return dailyCounts.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString().substring(5),
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
            ));
    }

    public TeamStatsResponse getTeamStatistics() {
        TeamStatsResponse statsDto = new TeamStatsResponse();
        statsDto.setTotalTeams(teamRepository.countByActivatedTrue());
        statsDto.setActiveTeams(
            teamRepository.countByStatusInAndActivatedTrue(
                List.of(Status.FULL, Status.RECRUITING)));
        statsDto.setCompletedTeams(
            teamRepository.countByStatusInAndActivatedTrue(List.of(Status.COMPLETED)));
        statsDto.setNewTeamsLast30Days(
            teamRepository.countByCreatedAtAfterAndActivatedTrue(
                LocalDateTime.now().minusDays(30)));
        double averageSize = teamRepository.averageMaxPeopleForActiveTeams();
        statsDto.setAverageTeamSize(averageSize);
        return statsDto;
    }

    // 팀 참여자 조회 및 해당 유저별 맛집 목록 불러오기
    public List<Map<String, Object>> getParticipantMymapData(Long teamId) {
        List<Participant> participants = participantRepository.findParticipantsWithUserByTeamId(
                teamId).stream()
            .filter(p -> p.getParticipantStatus() == ParticipantStatus.APPROVED)
            .toList();

        Map<String, User> userMap = participants.stream()
            .collect(Collectors.toMap(p -> p.getUser().getUserId(), Participant::getUser));

        List<Mymap> allMymaps = mymapRepository.findActivatedMymapsByUserListAndPinned(
            userMap.values(), true);

        Map<String, List<Mymap>> mymapsByUser = allMymaps.stream()
            .collect(Collectors.groupingBy(m -> m.getUser().getUserId()));

        return participants.stream()
            .map(p -> {
                User user = p.getUser();
                List<Map<String, Object>> restaurants = mymapsByUser.getOrDefault(user.getUserId(),
                        List.of())
                    .stream()
                    .map(m -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("mapId", m.getMapId());
                        map.put("name", m.getPlaceName());
                        map.put("roadAddress", m.getRoadAddress());
                        map.put("latitude", m.getLatitude());
                        map.put("longitude", m.getLongitude());
                        map.put("memo", m.getMemo());
                        return map;
                    })
                    .collect(Collectors.toList());

                return Map.of(
                    "userId", user.getUserId(),
                    "nickname", user.getNickname(),
                    "restaurants", restaurants
                );
            })
            .toList();
    }

    public Map<String, Integer> getUserStats(String userId) {
        Map<String, Integer> stats = new HashMap<>();

        List<Team> leaderTeams = getTeamsByLeader(userId);
        stats.put("leaderCount", leaderTeams.size());

        List<Team> participatingTeams = getTeamsByParticipant(userId);
        participatingTeams.removeAll(leaderTeams);
        stats.put("participatingCount", participatingTeams.size());

        List<Team> completedTeams = getAllTeamsForUser(userId).stream()
            .filter(team -> team.getStatus() == Status.COMPLETED)
            .collect(Collectors.toList());
        stats.put("completedCount", completedTeams.size());

        return stats;
    }

    //주최자 모임 삭제
    @Transactional
    public void unActivatedTeamByLeader(Long teamId, String userId) {
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다."));

        team.setStatus(Status.CANCELED);
        unActivatedById(teamId);

        teamRepository.save(team);

        List<Participant> participants = team.getParticipants();
        for (Participant participant : participants) {
            if (participant.getParticipantStatus() == ParticipantStatus.APPROVED) {
                notificationSender.sendNotificationToUser(participant.getUser().getUserId(),
                    NotificationType.TEAM_STATUS, "[" + team.getTeamTitle() + "] 모임이 삭제되었습니다.",
                    null);
            }
        }
    }

    public List<SearchTeamResponse> getParticipantByLeader(String keyword) {
        return teamRepository.findTeamByKeyword(keyword);
    }

    public List<SearchUserResponse> getUserByUserId(String keyword) {
        return userRepository.findUserByKeyword(keyword);
    }

    @Transactional
    public void cancelRequest(String userId, Long teamId) {
        Participant participant = participantRepository.findByUser_UserIdAndTeam_TeamId(userId,
            teamId);
        if (participant == null) {
            throw new IllegalStateException("참가 신청 내역이 없습니다.");
        }

        if (participant.getParticipantStatus() != ParticipantStatus.PENDING) {
            throw new IllegalStateException("이미 수락된 경우 취소 불가");
        }

        participantRepository.delete(participant);
    }
}
