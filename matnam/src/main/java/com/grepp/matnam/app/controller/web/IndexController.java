package com.grepp.matnam.app.controller.web;

import com.grepp.matnam.app.model.content.entity.ContentRanking;
import com.grepp.matnam.app.model.content.entity.RankingItem;
import com.grepp.matnam.app.model.content.service.ContentRankingService;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.service.CouponManageService;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final ContentRankingService contentRankingService;
    private final CouponManageService couponManageService;
    private final TeamService teamService;

    @GetMapping("/")
    public String index(Model model) {
        Optional<ContentRanking> activeRanking = contentRankingService.getCurrentActiveRanking();

        List<Team> topTeams = teamService.getTop3TeamsByFavoriteCount();
        model.addAttribute("topTeams", topTeams);

        // 랭킹 가져오기
        if (activeRanking.isPresent()) {
            ContentRanking ranking = activeRanking.get();
            model.addAttribute("contentRanking", ranking);

            List<RankingItem> items = contentRankingService.getActiveRankingItems(ranking);
            if (!items.isEmpty()) {
                model.addAttribute("rankingItems", items);
            }
        }

        try {
            PageRequest latestCoupons = PageRequest.of(0, 3, Sort.by(Sort.Order.desc("createdAt")));
            Page<CouponTemplate> couponPage = couponManageService.findAvailableCoupons("", latestCoupons);
            model.addAttribute("latestCoupons", couponPage.getContent());
        } catch (Exception e) {
            model.addAttribute("latestCoupons", List.of());
        }

        return "index";
    }
}