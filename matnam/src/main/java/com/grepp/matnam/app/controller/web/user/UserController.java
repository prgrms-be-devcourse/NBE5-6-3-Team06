package com.grepp.matnam.app.controller.web.user;

import com.grepp.matnam.app.controller.api.auth.payload.TokenResponse;
import com.grepp.matnam.app.model.auth.service.AuthService;
import com.grepp.matnam.app.model.auth.token.dto.TokenDto;
import com.grepp.matnam.app.controller.web.user.payload.CouponStatsDto;
import com.grepp.matnam.app.model.coupon.service.UserCouponService;
import com.grepp.matnam.app.model.mymap.service.MymapService;
import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.team.dto.TeamDto;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.service.FavoriteService;
import com.grepp.matnam.app.model.team.service.TeamReviewService;
import com.grepp.matnam.app.model.team.service.TeamService;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.service.UserService;
import com.grepp.matnam.infra.auth.AuthenticationUtils;
import com.grepp.matnam.infra.auth.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final TeamService teamService;
    private final TeamReviewService teamReviewService;
    private final MymapService mymapService;
    private final FavoriteService favoriteService;
    private final AuthService authService;
    private final UserCouponService userCouponService;

    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup";
    }

    @GetMapping("/signin")
    public String signin() {
        return "user/signin";
    }

    @GetMapping("/preference")
    public String preference(HttpServletRequest request) {
        if (CookieUtils.getAccessToken(request).isEmpty() && !AuthenticationUtils.isAuthenticated()) {
            return "redirect:/user/signin";
        }

        return "user/preference";
    }

    @Transactional
    @GetMapping("/mypage")
    public String mypage(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "5") int size,
                         @RequestParam(defaultValue = "0") int teamPage,
                         @RequestParam(defaultValue = "5") int teamSize) {
        log.info("마이페이지 접근 시도");

        if (!AuthenticationUtils.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 마이페이지 접근 시도");
            return "redirect:/user/signin";
        }

        try {
            String userId = AuthenticationUtils.getCurrentUserId();
            log.info("인증된 사용자 ID: {}", userId);

            User user = userService.getUserById(userId);
            log.info("사용자 정보 조회 성공: {}", user.getUserId());

            CouponStatsDto couponStats = userCouponService.getCouponStats(userId);
            model.addAttribute("couponStats", couponStats);

            // 맛집 저장 수
            Map<String, Long> placeCounts = mymapService.getPlaceCounts(user);
            model.addAttribute("visiblePlaceCount", placeCounts.get("visible"));
            model.addAttribute("hiddenPlaceCount", placeCounts.get("hidden"));

            // 통계 데이터
            model.addAttribute("stats", teamService.getUserStats(userId));

            // 리뷰 정보
            List<Team> completedTeams = teamService.getTeamsByParticipant(userId).stream()
                    .filter(team -> team.getStatus() == Status.COMPLETED)
                    .toList();

            List<Team> teamsWithoutReview = new ArrayList<>();
            for (Team team : completedTeams) {
                boolean hasCompletedAllReviews = teamReviewService.hasUserCompletedAllReviews(team.getTeamId(), userId);
                if (!hasCompletedAllReviews) {
                    teamsWithoutReview.add(team);
                }
            }

            int totalTeamsWithoutReview = teamsWithoutReview.size();
            int totalPages = (int) Math.ceil((double) totalTeamsWithoutReview / size);

            int start = page * size;
            int end = Math.min(start + size, totalTeamsWithoutReview);

            List<Team> paginatedTeamsWithoutReview =
                    start < totalTeamsWithoutReview ? teamsWithoutReview.subList(start, end) : new ArrayList<>();

            model.addAttribute("user", user);
            model.addAttribute("teamsWithoutReview", paginatedTeamsWithoutReview);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);

            // 주최한 모임 조회
            List<Team> hostingTeams = teamService.getTeamsByLeader(userId);

            // 참여 중인 모임 조회 (승인된 참여자 상태)
            List<Team> participatingTeams = teamService.getTeamsByParticipant(userId);
            participatingTeams.removeAll(hostingTeams);

            // 참여한 모든 모임 조회 (승인된 참여자 및 상태 무관)
            List<Team> allTeams = teamService.getAllTeams(userId);

            // 즐겨찾기 조회
            List<TeamDto> favoriteTeams = favoriteService.getFavoriteForUser(userId);

            for (TeamDto dto : favoriteTeams) {
                long cnt = favoriteService.countFavoritesByTeamId(dto.getTeamId());
                dto.setFavoriteCount(cnt);
            }

            favoriteTeams.sort(
                Comparator.comparingLong(TeamDto::getFavoriteCount).reversed()
            );

            hostingTeams.sort(getTeamComparator());
            participatingTeams.sort(getTeamComparator());
            allTeams.sort(getTeamComparator());


            // todo 겹치는 코드 리팩토링
            int allTotal = allTeams.size();
            int hostingTotal = hostingTeams.size();
            int partTotal = participatingTeams.size();
            int favTotal = favoriteTeams.size();

            int allPages = (int)Math.ceil((double)allTotal / teamSize);
            int hostingPages = (int)Math.ceil((double)hostingTotal / teamSize);
            int partPages = (int)Math.ceil((double)partTotal / teamSize);
            int favPages = (int)Math.ceil((double)favTotal / teamSize);

            int allStart = teamPage * teamSize;
            int allEnd = Math.min(allStart + teamSize, allTotal);
            int hostStart = teamPage * teamSize;
            int hostEnd = Math.min(hostStart + teamSize, hostingTotal);
            int partStart = teamPage * teamSize;
            int partEnd = Math.min(partStart + teamSize, partTotal);
            int favStart = teamPage * teamSize;
            int favEnd = Math.min(favStart + teamSize, favTotal);

            List<Team> paginatedAll  = allStart < allTotal
                ? allTeams.subList(allStart, allEnd)
                : Collections.emptyList();
            List<Team> paginatedHost = hostStart < hostingTotal
                ? hostingTeams.subList(hostStart, hostEnd)
                : Collections.emptyList();
            List<Team> paginatedPart = partStart < partTotal
                ? participatingTeams.subList(partStart, partEnd)
                : Collections.emptyList();
            List<TeamDto> paginatedFav = favStart < favTotal
                ? favoriteTeams.subList(favStart, favEnd)
                : Collections.emptyList();

            model.addAttribute("allTeams", paginatedAll);
            model.addAttribute("allPages", allPages);

            model.addAttribute("hostingTeams", paginatedHost);
            model.addAttribute("hostingPages", hostingPages);

            model.addAttribute("participatingTeams", paginatedPart);
            model.addAttribute("participatingPages", partPages);

            model.addAttribute("favoriteTeams", paginatedFav);
            model.addAttribute("favoritePages", favPages);

            model.addAttribute("teamCurrentPage", teamPage);
            model.addAttribute("teamSize", teamSize);
            model.addAttribute("userMaps", new ArrayList<>());



            return "user/mypage";
        } catch (Exception e) {
            log.error("마이페이지 로딩 중 오류 발생: {}", e.getMessage(), e);
            return "redirect:/user/signin?error=profile_load_failed";
        }
    }

    public static Comparator<Team> getTeamComparator() {
        return Comparator
                .comparing((Team team) -> {
                    if (team.getStatus() == Status.RECRUITING) {
                        return 1;
                    } else if (team.getStatus() == Status.FULL) {
                        return 2;
                    } else if (team.getStatus() == Status.COMPLETED) {
                        return 3;
                    } else if (team.getStatus() == Status.CANCELED) {
                        return 4;
                    }
                    return 5;
                })
                .thenComparing(Team::getTeamDate);
    }

    @PostMapping("/deactivate")
    public String deactivateAccount(
            @RequestParam("password") String password,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String currentUserId = AuthenticationUtils.getCurrentUserId();
            userService.deactivateAccount(currentUserId, password);

            CookieUtils.clearAllCookies(request, response);

            AuthenticationUtils.clearSecurityContext();

            return "redirect:/?message=account_deactivated";
        } catch (Exception e) {
            return "redirect:/user/mypage?error=" + e.getMessage();
        }
    }

    @GetMapping("/password/change")
    public String passwordChange() {
        if (!AuthenticationUtils.isAuthenticated()) {
            return "redirect:/user/signin";
        }

        return "user/passwordChange";
    }

    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String code, HttpServletResponse response) {
        try{
            // 1. 인증 처리
            User user = userService.activateUserByEmailCode(code);

            // 2. 자동 로그인 (쿠키에 JWT 저장)
            TokenDto dto = authService.processTokenSignin(user.getUserId());
            setAuthCookies(response, dto);

            // 3. 인증 완료 -> 취향 페이지로
            return "redirect:/user/preference";
        }catch (Exception e) {
            log.error("이메일 인증 중 오류: {}", e.getMessage());
            throw e;
        }
    }
    private void setAuthCookies(HttpServletResponse response, TokenDto dto) {
        try {
            CookieUtils.addTokenCookie(response, "ACCESS_TOKEN", dto.getAccessToken(), "/");
            CookieUtils.addTokenCookie(response, "REFRESH_TOKEN", dto.getRefreshToken(), "/");
            log.debug("토큰 쿠키 설정 완료");
        } catch (Exception e) {
            log.error("토큰 쿠키 설정 실패: {}", e.getMessage());
            throw e;
        }
    }
}