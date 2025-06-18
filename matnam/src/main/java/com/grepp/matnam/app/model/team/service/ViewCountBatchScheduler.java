package com.grepp.matnam.app.model.team.service;

import com.grepp.matnam.app.model.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewCountBatchScheduler {

    private final ViewCountService viewCountService;
    private final TeamRepository teamRepository;

    @Transactional
    @Scheduled(fixedRate = 1000) // 5분 주기
    public void syncViewCountsToDatabase() {
        Map<Long, Long> viewCounts = viewCountService.getAllViewCounts();

        for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
            Long teamId = entry.getKey();
            Long count = entry.getValue();

            teamRepository.increaseViewCountBy(teamId, count);
            viewCountService.clearViewCount(teamId);
        }
    }
}