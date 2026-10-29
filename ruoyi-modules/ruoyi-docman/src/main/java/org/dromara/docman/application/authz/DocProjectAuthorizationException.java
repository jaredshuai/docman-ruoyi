package org.dromara.docman.application.authz;

import org.springframework.http.HttpStatus;

/**
 * 项目资源授权边界的受控异常，携带统一错误边界所需的 HTTP 状态。
 */
public class DocProjectAuthorizationException extends RuntimeException {

    private final HttpStatus status;

    public DocProjectAuthorizationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static DocProjectAuthorizationException unauthenticated(String message) {
        return new DocProjectAuthorizationException(HttpStatus.UNAUTHORIZED, message);
    }

    public static DocProjectAuthorizationException forbidden(String message) {
        return new DocProjectAuthorizationException(HttpStatus.FORBIDDEN, message);
    }

    public static DocProjectAuthorizationException unavailable(String message) {
        return new DocProjectAuthorizationException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
