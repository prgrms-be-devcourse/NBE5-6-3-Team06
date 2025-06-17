package com.grepp.matnam.app.model.team.service;

import com.grepp.matnam.app.model.team.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;

    private final TeamRepository teamRepository;


    public ViewCountService(
            @Qualifier("stringKeyRedisTemplate") RedisTemplate<String, String> redisTemplate,
            TeamRepository teamRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.teamRepository = teamRepository;
    }

    public void increaseViewCount(Long teamId) {
        String key = "team:viewcount:" + teamId;
        redisTemplate.opsForValue().increment(key, 1);
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void syncViewCountsFromRedis() {
        Set<String> keys = redisTemplate.keys("team:viewcount:*");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            try {
                String teamIdStr = key.replace("team:viewcount:", "");
                Long teamId = Long.parseLong(teamIdStr);
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    long increment = Long.parseLong(value);
                    if (increment > 0) {
                        teamRepository.updateViewCount(teamId, increment);
                        redisTemplate.delete(key);
                        log.info("조회수 동기화: teamId={}, 증가분={}", teamId, increment);
                    }
                }
            } catch (Exception e) {
                log.error("조회수 동기화 실패 (key: {})", key, e);
            }
        }
    }
}
