package org.dromara.web.service.identity.oidc.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的短生命周期登录事务存储。
 * <p>
 * 每个 state 使用独立键，保存 JSON 字符串并设置 TTL；回调时通过
 * {@link RBucket#getAndDelete()} 原子消费，重复回调或过期 state 一律返回空。
 * 存储层故障按 provider 不可用处理（503），不降级为内存或客户端状态。
 */
public class RedisOidcLoginTransactionStore implements OidcLoginTransactionStore {

    static final String KEY_PREFIX = "docman:oidc:login:";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RedissonClient redissonClient;

    public RedisOidcLoginTransactionStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean save(String state, OidcLoginTransaction transaction, Duration ttl) {
        if (state == null || state.isBlank() || transaction == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return false;
        }
        try {
            String payload = MAPPER.writeValueAsString(transaction);
            return bucket(state).setIfAbsent(payload, ttl);
        } catch (JsonProcessingException ex) {
            throw OidcLoginException.unavailable("OIDC login state could not be stored");
        } catch (RuntimeException ex) {
            throw OidcLoginException.unavailable("OIDC login state store is unavailable");
        }
    }

    @Override
    public Optional<OidcLoginTransaction> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        try {
            String payload = bucket(state).getAndDelete();
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(MAPPER.readValue(payload, OidcLoginTransaction.class));
        } catch (JsonProcessingException ex) {
            throw OidcLoginException.unauthenticated("OIDC login state is invalid");
        } catch (RuntimeException ex) {
            throw OidcLoginException.unavailable("OIDC login state store is unavailable");
        }
    }

    private RBucket<String> bucket(String state) {
        return redissonClient.getBucket(KEY_PREFIX + state, StringCodec.INSTANCE);
    }
}
