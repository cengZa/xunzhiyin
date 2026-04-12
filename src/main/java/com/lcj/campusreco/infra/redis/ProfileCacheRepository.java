package com.lcj.campusreco.infra.redis;

import com.lcj.campusreco.common.constant.RedisKeys;
import com.lcj.campusreco.domain.model.UserProfileModel;
import java.time.Duration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public ProfileCacheRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public UserProfileModel get(Long userId) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(userId));
            return value instanceof UserProfileModel profileModel ? profileModel : null;
        } catch (Exception exception) {
            return null;
        }
    }

    public void save(Long userId, UserProfileModel profileModel) {
        try {
            redisTemplate.opsForValue().set(buildKey(userId), profileModel, Duration.ofHours(1));
        } catch (Exception exception) {
            // Ignore cache failures so the main recommendation flow can continue.
        }
    }

    public void evict(Long userId) {
        try {
            redisTemplate.delete(buildKey(userId));
        } catch (Exception exception) {
            // Ignore cache failures so the main recommendation flow can continue.
        }
    }

    private String buildKey(Long userId) {
        return RedisKeys.PROFILE_CACHE_PREFIX + userId;
    }
}
