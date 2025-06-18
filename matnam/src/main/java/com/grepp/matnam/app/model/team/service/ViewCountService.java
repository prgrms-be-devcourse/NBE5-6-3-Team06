package com.grepp.matnam.app.model.team.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ViewCountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String VIEW_COUNT_KEY_PREFIX = "team:viewCount:";

    public void increaseViewCount(Long teamId) {
        String key = VIEW_COUNT_KEY_PREFIX + teamId;
        redisTemplate.opsForValue().increment(key);
    }

    public Long getViewCount(Long teamId) {
        String key = VIEW_COUNT_KEY_PREFIX + teamId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) return 0L;
        return Long.parseLong(value.toString());
    }

    public Map<Long, Long> getAllViewCounts() {
        Set<String> keys = redisTemplate.keys(VIEW_COUNT_KEY_PREFIX + "*");
        Map<Long, Long> result = new HashMap<>();
        if (keys != null) {
            for (String key : keys) {
                String teamIdStr = key.replace(VIEW_COUNT_KEY_PREFIX, "");
                Object val = redisTemplate.opsForValue().get(key);
                if (val != null) {
                    result.put(Long.parseLong(teamIdStr), Long.parseLong(val.toString()));
                }
            }
        }
        return result;
    }

    public void clearViewCount(Long teamId) {
        redisTemplate.delete(VIEW_COUNT_KEY_PREFIX + teamId);
    }
}