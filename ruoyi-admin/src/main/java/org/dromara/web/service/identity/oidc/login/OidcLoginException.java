package org.dromara.web.service.identity.oidc.login;

import org.springframework.http.HttpStatus;

/**
 * OIDC 登录边界的受控异常，携带统一错误边界所需的 HTTP 状态。
 * <p>
 * 消息只包含稳定的业务描述，不包含 client secret、token、provider 响应体或内部异常文本。
 */
public class OidcLoginException extends RuntimeException {

    private final HttpStatus status;

    public OidcLoginException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static OidcLoginException unauthenticated(String message) {
        return new OidcLoginException(HttpStatus.UNAUTHORIZED, message);
    }

    public static OidcLoginException forbidden(String message) {
        return new OidcLoginException(HttpStatus.FORBIDDEN, message);
    }

    public static OidcLoginException unavailable(String message) {
        return new OidcLoginException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
