package org.dromara.docman.application.authz;

import com.jxccs.authz.AuthorizationClient;
import com.jxccs.authz.ClientConfig;
import com.jxccs.authz.RawResponse;
import com.jxccs.authz.Transport;
import com.jxccs.authz.TransportException;
import org.dromara.docman.domain.vo.DocProjectVo;
import org.dromara.docman.infrastructure.authz.DocmanAuthorizationClientProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
@Tag("prod")
@Tag("local")
class DocProjectAuthorizationServiceTest {

    private static final String ISSUER = "casdoor-jiaxing-company";
    private static final String SUBJECT = "casdoor-subject-001";
    private static final String OWNER_ISSUER = "casdoor-jiaxing-company";
    private static final String OWNER_SUBJECT = "casdoor-owner-009";
    private static final String TENANT_ID = "000000";
    private static final Long PROJECT_ID = 42L;
    private static final Long OWNER_ID = 9L;

    @Mock
    private CanonicalPrincipalProvider principalProvider;
    @Mock
    private OwnerSubjectResolver ownerSubjectResolver;
    @Mock
    private DocmanAuthorizationClientProvider clientProvider;

    private RecordingTransport transport;
    private DocProjectAuthorizationService service;

    @BeforeEach
    void setUp() {
        transport = new RecordingTransport(request -> decision(request, true, "SUBJECT_GRANT"));
        AuthorizationClient client = new AuthorizationClient(clientConfig(), transport);
        service = new DocProjectAuthorizationService(principalProvider, ownerSubjectResolver, clientProvider);
        lenient().when(principalProvider.current()).thenReturn(Optional.of(new CanonicalPrincipal(ISSUER, SUBJECT)));
        lenient().when(principalProvider.currentTenantId()).thenReturn(Optional.of(TENANT_ID));
        lenient().when(ownerSubjectResolver.resolve(OWNER_ID, TENANT_ID))
            .thenReturn(Optional.of(new CanonicalPrincipal(OWNER_ISSUER, OWNER_SUBJECT)));
        lenient().when(clientProvider.client()).thenReturn(Optional.of(client));
    }

    @Test
    void rejectsMissingCanonicalPrincipalWith401BeforeCallingAuthorizationCenter() {
        when(principalProvider.current()).thenReturn(Optional.empty());

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(ownerSubjectResolver);
        verifyNoInteractions(clientProvider);
    }

    @Test
    void missingOwnerBindingReturns403BeforeCallingAuthorizationCenter() {
        when(ownerSubjectResolver.resolve(OWNER_ID, TENANT_ID)).thenReturn(Optional.empty());

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(clientProvider);
    }

    @Test
    void missingSessionTenantReturns403BeforeCallingAuthorizationCenter() {
        when(principalProvider.currentTenantId()).thenReturn(Optional.empty());

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(ownerSubjectResolver);
        verifyNoInteractions(clientProvider);
    }

    @Test
    void ownerResolverFailureReturns403BeforeCallingAuthorizationCenter() {
        when(ownerSubjectResolver.resolve(OWNER_ID, TENANT_ID))
            .thenThrow(new IllegalStateException("binding lookup failed"));

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(clientProvider);
    }

    @Test
    void projectWithoutOwnerReturns403BeforeCallingAuthorizationCenter() {
        DocProjectVo project = project();
        project.setOwnerId(null);

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(ownerSubjectResolver);
        verifyNoInteractions(clientProvider);
    }

    @Test
    void returns503WhenAuthorizationServiceIsNotConfigured() {
        when(clientProvider.client()).thenReturn(Optional.empty());

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        assertEquals(0, transport.calls);
    }

    @Test
    void allowedDecisionKeepsOriginalReadResultAndSendsOnlyOwnerSubject() {
        DocProjectVo project = project();

        assertDoesNotThrow(() -> service.checkRead(PROJECT_ID, project));

        Map<String, Object> body = transport.lastRequest;
        Map<?, ?> subject = (Map<?, ?>) body.get("subject");
        Map<?, ?> resource = (Map<?, ?>) body.get("resource");
        Map<?, ?> attributes = (Map<?, ?>) resource.get("attributes");
        Map<?, ?> ownerSubject = (Map<?, ?>) attributes.get("ownerSubject");
        assertAll(
            () -> assertEquals(1, transport.calls),
            () -> assertEquals(ISSUER, subject.get("issuer")),
            () -> assertEquals(SUBJECT, subject.get("sub")),
            () -> assertEquals("docman:project:query", body.get("permission")),
            () -> assertEquals("project", resource.get("resourceType")),
            () -> assertEquals("42", resource.get("resourceId")),
            () -> assertEquals(Set.of("ownerSubject"), attributes.keySet()),
            () -> assertEquals(Map.of("issuer", OWNER_ISSUER, "sub", OWNER_SUBJECT), ownerSubject),
            () -> assertFalse(attributes.containsKey("ownerId")),
            () -> assertFalse(attributes.containsKey("memberIds")),
            () -> assertFalse(attributes.containsKey("currentUserRole")),
            () -> assertFalse(attributes.containsKey("status")),
            () -> assertFalse(attributes.containsKey("projectTypeCode"))
        );
        verify(ownerSubjectResolver).resolve(OWNER_ID, TENANT_ID);
    }

    @Test
    void explicitDenyReturns403() {
        transport.responder = request -> decision(request, false, "EXPLICIT_DENY");

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(1, transport.calls);
    }

    @Test
    void scopeMismatchReturns403() {
        transport.responder = request -> decision(request, false, "SCOPE_MISMATCH");

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(1, transport.calls);
    }

    @Test
    void missingGrantReturns403() {
        transport.responder = request -> decision(request, false, "MISSING_GRANT");

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(1, transport.calls);
    }

    @Test
    void authorizationCenterFailureReturns503WithoutGrantingAccess() {
        transport.responder = request -> {
            throw new TransportException("connection refused");
        };

        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, project()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.status());
        assertEquals(1, transport.calls);
    }

    @Test
    void nullProjectFailsClosedWithoutCallingAuthorizationCenter() {
        DocProjectAuthorizationException ex = assertThrows(DocProjectAuthorizationException.class,
            () -> service.checkRead(PROJECT_ID, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.status());
        assertEquals(0, transport.calls);
        verifyNoInteractions(ownerSubjectResolver);
        verifyNoInteractions(clientProvider);
    }

    private static DocProjectVo project() {
        DocProjectVo project = new DocProjectVo();
        project.setId(PROJECT_ID);
        project.setStatus("ACTIVE");
        project.setOwnerId(OWNER_ID);
        project.setProjectTypeCode("TELECOM");
        project.setCurrentUserRole("OWNER");
        project.setMemberIds(java.util.Arrays.asList(3L, null, 5L));
        return project;
    }

    private static ClientConfig clientConfig() {
        return ClientConfig.builder()
            .mode(ClientConfig.Mode.LOCAL)
            .provider(ClientConfig.Provider.FIXTURE)
            .application("docman")
            .environment("local")
            .fixtureScenario("project-detail")
            .build();
    }

    private static RawResponse decision(Map<String, Object> request, boolean allowed, String reasonCode) {
        String requestId = String.valueOf(request.get("requestId"));
        String body = "{"
            + "\"allowed\":" + allowed + ","
            + "\"decisionId\":\"decision-1\","
            + "\"policyVersion\":\"pv-1\","
            + "\"entitlementVersion\":7,"
            + "\"reasonCodes\":[\"" + reasonCode + "\"],"
            + "\"evaluatedAt\":\"2026-10-21T02:00:00Z\","
            + "\"requestId\":\"" + requestId + "\""
            + "}";
        return RawResponse.json(200, body);
    }

    private static final class RecordingTransport implements Transport {

        private Function<Map<String, Object>, RawResponse> responder;
        private Map<String, Object> lastRequest;
        private int calls;

        private RecordingTransport(Function<Map<String, Object>, RawResponse> responder) {
            this.responder = responder;
        }

        @Override
        public RawResponse postJson(String path, Map<String, Object> body) {
            calls++;
            lastRequest = new LinkedHashMap<>(body);
            return responder.apply(body);
        }
    }
}
