package org.dromara.web.service.identity.oidc.login;

import java.time.Duration;
import java.util.Optional;

/**
 * state/nonce/PKCE verifier 的服务端短生命周期存储。
 */
public interface OidcLoginTransactionStore {

    /**
     * 保存事务；相同 state 已存在时不覆盖并返回 {@code false}。
     */
    boolean save(String state, OidcLoginTransaction transaction, Duration ttl);

    /**
     * 原子读取并删除事务；不存在、已过期或已消费时返回 {@link Optional#empty()}。
     */
    Optional<OidcLoginTransaction> consume(String state);
}
