package org.dromara.web.service.identity.oidc.login;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.satoken.utils.LoginHelper;

/**
 * 复用现有 {@link LoginHelper#login} 与 Sa-Token 会话的登录适配器。
 */
public class SaTokenOidcSessionAuthenticator implements OidcSessionAuthenticator {

    @Override
    public String login(LoginUser loginUser, String clientId) {
        SaLoginParameter parameter = new SaLoginParameter();
        parameter.setExtra(LoginHelper.CLIENT_KEY, clientId);
        LoginHelper.login(loginUser, parameter);
        return StpUtil.getTokenValue();
    }
}
