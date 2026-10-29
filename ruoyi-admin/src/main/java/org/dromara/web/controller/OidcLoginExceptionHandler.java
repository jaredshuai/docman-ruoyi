package org.dromara.web.controller;

import org.dromara.common.core.domain.R;
import org.dromara.web.service.identity.oidc.login.OidcLoginException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * OIDC 登录端点的统一错误边界。
 * <p>
 * 未认证 401、绑定/授权拒绝 403、provider/网络/协议/配置缺失 503。
 */
@RestControllerAdvice(assignableTypes = OidcLoginController.class)
public class OidcLoginExceptionHandler {

    @ExceptionHandler(OidcLoginException.class)
    public ResponseEntity<R<Void>> handleOidcLoginException(OidcLoginException e) {
        int status = e.status().value();
        return ResponseEntity.status(e.status()).body(R.fail(status, e.getMessage()));
    }
}
