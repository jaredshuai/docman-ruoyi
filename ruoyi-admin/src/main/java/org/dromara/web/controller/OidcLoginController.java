package org.dromara.web.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.web.domain.vo.LoginVo;
import org.dromara.web.service.SysLoginService;
import org.dromara.web.service.identity.oidc.login.OidcLoginResult;
import org.dromara.web.service.identity.oidc.login.OidcLoginService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Casdoor OIDC 登录入口。
 * <p>
 * 只负责跳转、回调编排和本地会话退出；不读取请求参数、Cookie 或普通 Header 作为 Principal。
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/oidc")
public class OidcLoginController {

    private final OidcLoginService loginService;
    private final SysLoginService sysLoginService;

    /**
     * 生成 state/nonce/PKCE 并跳转到 Casdoor authorization endpoint。
     */
    @GetMapping("/login/start")
    public ResponseEntity<Void> start() {
        String authorizationUrl = loginService.buildAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authorizationUrl)).build();
    }

    /**
     * 处理 Casdoor 回调；state 为一次性，成功后才建立本地会话。
     */
    @GetMapping("/callback")
    public R<LoginVo> callback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               @RequestParam(required = false) String error) {
        OidcLoginResult result = loginService.callback(code, state, error);
        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(result.accessToken());
        loginVo.setClientId(result.clientId());
        loginVo.setScope(result.scope());
        return R.ok(loginVo);
    }

    /**
     * 只退出本地会话；未配置或未实现 provider logout 时不声称 Casdoor 已退出。
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        sysLoginService.logout();
        return R.ok("本地会话已退出");
    }
}
