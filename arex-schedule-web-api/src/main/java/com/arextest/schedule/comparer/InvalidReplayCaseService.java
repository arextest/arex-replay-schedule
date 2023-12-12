package com.arextest.schedule.comparer;

import com.arextest.common.cache.CacheProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * listen agent service query storage service if exception mark replay case invalid
 * @author: sldu
 * @date: 2023/12/6 13:46
 **/
@Component
@Slf4j
public class InvalidReplayCaseService {
    @Resource
    private CacheProvider redisCacheProvider;
    private static final byte[] INVALID_CASE_KEY = "invalid_case".getBytes(StandardCharsets.UTF_8);
    private static final byte[] INVALID_CASE_VALUE = "1".getBytes(StandardCharsets.UTF_8);
    private static final long THREE_MINUTES_EXPIRE = 3 * 60L;

    public void saveInvalidCase(String replayId) {
        byte[] key = toInvalidCaseKeyBytes(replayId);
        redisCacheProvider.put(key, THREE_MINUTES_EXPIRE, INVALID_CASE_VALUE);
    }
    public boolean isInvalidCase(String replayId) {
        byte[] key = toInvalidCaseKeyBytes(replayId);
        try {
            byte[] bytes = redisCacheProvider.get(key);
            if (bytes != null) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("isInvalidCase error", e);
        }
        return false;
    }
    private byte[] toInvalidCaseKeyBytes(String replayId) {
        byte[] paramKey = replayId.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(INVALID_CASE_KEY.length + paramKey.length).put(INVALID_CASE_KEY).put(paramKey).array();
    }
}