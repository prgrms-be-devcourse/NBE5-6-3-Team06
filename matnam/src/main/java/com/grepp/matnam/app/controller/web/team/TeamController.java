package com.grepp.matnam.app.controller.web.team;

import com.grepp.matnam.app.controller.api.team.payload.TeamRequest;
import com.grepp.matnam.app.controller.api.team.payload.UpdatedTeamRequest;
import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.code.Role;
import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.entity.Participant;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.entity.TeamReview;
import com.grepp.matnam.app.model.team.repository.ParticipantRepository;
import com.grepp.matnam.app.model.team.repository.TeamReviewRepository;
import com.grepp.matnam.app.model.team.service.FavoriteService;
import com.grepp.matnam.app.model.team.service.StorageService;
import com.grepp.matnam.app.model.team.service.TeamService;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/team")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    @Value("${app.default-team-image}")
    private String defaultImageUrl;

    private final TeamService teamService;
    private final UserService userService;
    private final TeamReviewRepository teamReviewRepository;
    private final ParticipantRepository participantRepository;
    private final FavoriteService favoriteService;
    private final StorageService storageService;


    // 모임 생성 페이지
    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("teamRequest", new TeamRequest());
        model.addAttribute("defaultImageUrl", defaultImageUrl);
        return "team/teamCreate";
    }

    // 모임 생성
    @PostMapping("/create")
    public String createTeam(@Valid @ModelAttribute TeamRequest teamRequest,
        BindingResult bindingResult
    ) throws IOException {
        if (bindingResult.hasErrors()) {
            return "team/teamCreate";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        User user = userService.getUserById(userId);

        String imageUrl  = defaultImageUrl;

        // 업로드한 파일이 있으면 덮어쓰기
        MultipartFile file = teamRequest.getImageUrl();
        if (file != null && !file.isEmpty()) {
            imageUrl = storageService.store(file);
        }

        Team team = teamRequest.toEntity(user, imageUrl);

        if (teamRequest.getDate() != null && teamRequest.getTime() != null) {
            String dateTimeString = teamRequest.getDate() + "T" + teamRequest.getTime() + ":00";
            try {
                team.setTeamDate(LocalDateTime.parse(dateTimeString));
            } catch (Exception e) {
                team.setTeamDate(LocalDateTime.now());
            }
        } else {
            team.setTeamDate(LocalDateTime.now());
        }

        teamService.saveTeam(team);
        teamService.addParticipant(team.getTeamId(), user);

        return "redirect:/team/detail/" + team.getTeamId();
    }

    // 모임 수정 페이지
    @GetMapping("/edit/{teamId}")
    public String getTeamEditPage(@PathVariable Long teamId, Model model) {
        Team team = teamService.getTeamById(teamId);
        UpdatedTeamRequest dto = UpdatedTeamRequest.fromEntity(team);
        model.addAttribute("updatedTeamRequest", dto);
        model.addAttribute("team", team);
        model.addAttribute("defaultImageUrl", team.getImageUrl() != null
            ? team.getImageUrl()
            : defaultImageUrl);
        return "team/teamEdit";
    }

    // 모임 수정
    @PostMapping("/edit/{teamId}")
    public String updateTeam(@PathVariable Long teamId,
        @ModelAttribute UpdatedTeamRequest updatedTeamRequest,
        BindingResult bindingResult, Model model
    ) throws IOException {
        if (bindingResult.hasErrors()) {
            String existingUrl = teamService.getTeamById(teamId).getImageUrl();
            model.addAttribute("defaultImageUrl", existingUrl);
            return "team/teamEdit";
        }
        String UserId = SecurityContextHolder.getContext().getAuthentication().getName();

        Team existing = teamService.getTeamById(teamId);
        if (existing == null) {
            throw new EntityNotFoundException("모임을 찾을 수 없습니다.");
        }
        String imageUrl = existing.getImageUrl();
        MultipartFile file = updatedTeamRequest.getImageUrl();
        if (file != null && !file.isEmpty()) {
            imageUrl = storageService.store(file);
        }

        Team toUpdate = updatedTeamRequest.toTeam(imageUrl);
        toUpdate.setTeamId(teamId);

        teamService.updateTeam(teamId, toUpdate, UserId);
        return "redirect:/team/detail/" + teamId;
    }

    // 모임 검색 페이지
    @GetMapping("/search")
    public String searchTeams(
        @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "createdAt")
        String sort,
        @RequestParam(name = "includeCompleted", defaultValue = "true")
        boolean includeCompleted,
        Model model) {
        Page<Team> page;

        if ("favoriteCount".equals(sort)) {
            Pageable favPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "favoriteCount")
            );
            page = teamService.getAllTeamsByFavoriteCount(favPageable, includeCompleted);

        } else {
            page = teamService.getAllTeams(pageable, includeCompleted);
        }

        model.addAttribute("teams", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("sort", sort);
        model.addAttribute("includeCompleted", includeCompleted);
        return "team/teamSearch";
    }

    // 모임 상세 조회
    @GetMapping("/detail/{teamId}")
    public String teamDetail(@PathVariable Long teamId, Model model) {
        Team team = teamService.getTeamByIdWithParticipants(teamId);
        model.addAttribute("team", team);

        // approved 상태인 참가자들만 가져와서 리스트로 변환 - 뷰로 사용
        List<Participant> approvedParticipants = team.getParticipants().stream()
            .filter(participant -> participant.getParticipantStatus() == ParticipantStatus.APPROVED)
            .toList();
        model.addAttribute("participants", approvedParticipants);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = authentication.getPrincipal().equals("anonymousUser");

        if (isAnonymous) {
            model.addAttribute("isLeader", false);
            model.addAttribute("isParticipant", false);
            model.addAttribute("alreadyApplied", false);
            model.addAttribute("isAnonymous", true);
            return "team/teamDetail";
        }

        String userId = authentication.getName();
        User user = userService.getUserById(userId);

        boolean isLeader = team.getUser().getUserId().equals(userId);
        model.addAttribute("isLeader", isLeader);

        // 현재 사용자가 이미 팀에 참여했는지
        boolean isParticipant = participantRepository.existsByUser_UserIdAndTeam_TeamIdAndParticipantStatus(
            userId, teamId, ParticipantStatus.APPROVED);
        // 현재 사용자가 이미 신청했는지
        boolean alreadyApplied = participantRepository.existsByUser_UserIdAndTeam_TeamIdAndParticipantStatus(
            userId, teamId, ParticipantStatus.PENDING);

        model.addAttribute("isParticipant", isParticipant);
        model.addAttribute("alreadyApplied", alreadyApplied);
        model.addAttribute("user", user);
        model.addAttribute("isAnonymous", false);

        model.addAttribute("isFavorite", favoriteService.existsByUserAndTeam(userId, teamId));

        return "team/teamDetail";
    }


    // 팀 페이지 조회
    @GetMapping("/page/{teamId}")
    public String getTeamPage(@PathVariable Long teamId, Model model) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        User currentUser = userService.getUserById(userId);
        model.addAttribute("userId", userId);
        model.addAttribute("userNickname", currentUser.getNickname());
        model.addAttribute("teamId", teamId);

        Team team = teamService.getTeamByIdWithParticipants(teamId);
        if (team == null) {
            return "redirect:/error/404";
        }

        model.addAttribute("team", team);
        model.addAttribute("teamImageUrl", team.getImageUrl());

        if (team.getUser() != null) {
            model.addAttribute("leader", team.getUser());
        }

        List<Participant> approvedParticipants = team.getParticipants().stream()
            .filter(participant -> participant.getRole() == Role.MEMBER)
            .toList();
        model.addAttribute("participants", approvedParticipants);

        // 주최자인지 확인
        boolean isLeader = team.getUser().getUserId().equals(currentUser.getUserId());
        model.addAttribute("isLeader", isLeader);

        boolean isAdmin = currentUser.getRole()
            .equals(com.grepp.matnam.app.model.user.code.Role.ROLE_ADMIN);

        if (!isAdmin) {
            log.info("Checking if user {} is a participant in team {}", userId, teamId);
            Participant participant = participantRepository.findByUser_UserIdAndTeam_TeamId(userId,
                teamId);
            if (participant == null) {
                return "redirect:/team/" + teamId + "?error=notParticipant";
            }
        }

        return "team/teamPage";
    }


    // 모임 완료 후 리뷰 작성 페이지 표시
    @GetMapping("/{teamId}/reviews")
    public String showTeamReviewPage(@PathVariable Long teamId, Model model) {
        Team team = teamService.getTeamById(teamId);

        if (team == null) {
            return "redirect:/error/404";
        }

        if (team.getStatus() != Status.COMPLETED) {
            return "redirect:/team/" + teamId + "?error=notCompleted";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        Participant participant = participantRepository.findByUser_UserIdAndTeam_TeamId(
            currentUserId, teamId);
        if (participant == null) {
            return "redirect:/team/" + teamId + "?error=notParticipant";
        }

        List<Participant> participants = participantRepository.findParticipantsWithUserByTeamId(
            teamId);

        List<TeamReview> myReviews = teamReviewRepository.findByTeam_TeamIdAndReviewer(teamId,
            currentUserId);

        model.addAttribute("team", team);
        model.addAttribute("participants", participants);
        model.addAttribute("myReviews", myReviews);
        model.addAttribute("currentUserId", currentUserId);

        return "team/teamReview";
    }
}