package com.lcj.campusreco.infra.redis;

import com.lcj.campusreco.common.constant.RedisKeys;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecallIndexRepository {

    private final StringRedisTemplate stringRedisTemplate;

    public RecallIndexRepository(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Set<String> getCandidateUserIdsByTag(Long tagId) {
        if (tagId == null) {
            return Collections.emptySet();
        }
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(RedisKeys.RECALL_INVERTED_TAG_PREFIX + tagId);
            return members == null ? Collections.emptySet() : members;
        } catch (Exception exception) {
            return Collections.emptySet();
        }
    }

    public void replaceCandidateUserIdsByTag(Long tagId, Set<Long> userIds) {
        if (tagId == null) {
            return;
        }
        try {
            String key = RedisKeys.RECALL_INVERTED_TAG_PREFIX + tagId;
            stringRedisTemplate.delete(key);
            Set<String> values = new LinkedHashSet<>();
            for (Long userId : userIds) {
                values.add(String.valueOf(userId));
            }
            if (!values.isEmpty()) {
                stringRedisTemplate.opsForSet().add(key, values.toArray(String[]::new));
            }
        } catch (Exception exception) {
            // Ignore redis failures so the recommendation flow can degrade to DB fallback.
        }
    }
}
