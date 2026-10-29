package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.authz.DocProjectAuthorizationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 项目详情 PEP 的统一错误边界。
 * <p>
 * 无可信主体 401、显式拒绝/scope 不匹配 403、授权中心不可用或配置缺失 503。
 */
@RestControllerAdvice(assignableTypes = DocProjectController.class)
public class DocmanAuthorizationExceptionHandler {

    @ExceptionHandler(DocProjectAuthorizationException.class)
    public ResponseEntity<R<Void>> handleDocProjectAuthorizationException(DocProjectAuthorizationException e) {
        int status = e.status().value();
        return ResponseEntity.status(e.status()).body(R.fail(status, e.getMessage()));
    }
}
