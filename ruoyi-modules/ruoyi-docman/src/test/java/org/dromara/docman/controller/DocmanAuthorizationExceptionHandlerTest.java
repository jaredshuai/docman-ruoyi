package org.dromara.docman.controller;

import org.dromara.common.core.domain.R;
import org.dromara.docman.application.authz.DocProjectAuthorizationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("dev")
@Tag("prod")
@Tag("local")
class DocmanAuthorizationExceptionHandlerTest {

    private final DocmanAuthorizationExceptionHandler handler = new DocmanAuthorizationExceptionHandler();

    @Test
    void mapsMissingPrincipalTo401() {
        ResponseEntity<R<Void>> response = handler.handleDocProjectAuthorizationException(
            DocProjectAuthorizationException.unauthenticated("no principal"));

        assertBody(response, HttpStatus.UNAUTHORIZED);
    }

    @Test
    void mapsExplicitDenialTo403() {
        ResponseEntity<R<Void>> response = handler.handleDocProjectAuthorizationException(
            DocProjectAuthorizationException.forbidden("denied"));

        assertBody(response, HttpStatus.FORBIDDEN);
    }

    @Test
    void mapsAuthorizationCenterFailureTo503() {
        ResponseEntity<R<Void>> response = handler.handleDocProjectAuthorizationException(
            DocProjectAuthorizationException.unavailable("authorization service is unavailable"));

        assertBody(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static void assertBody(ResponseEntity<R<Void>> response, HttpStatus expected) {
        assertAll(
            () -> assertEquals(expected, response.getStatusCode()),
            () -> assertEquals(expected.value(), response.getBody().getCode()),
            () -> assertNotNull(response.getBody().getMsg())
        );
    }
}
