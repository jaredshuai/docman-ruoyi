package org.dromara.web.service.identity.oidc.login;

import org.dromara.common.core.domain.model.LoginUser;

/**
 * 将已验证并完成绑定的本地用户写入现有 Sa-Token 会话。
 */
public interface OidcSessionAuthenticator {

    /**
     * 登录并返回本地 access token。
     */
    String login(LoginUser loginUser, String clientId);
}
