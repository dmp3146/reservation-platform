package com.udea.reservations.platform.services.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.auth.security.DatabaseUserDetailsService;
import com.udea.reservations.platform.auth.security.SecurityConfiguration;
import com.udea.reservations.platform.auth.security.SecurityErrorResponseWriter;
import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.auth.controller.AuthController;
import com.udea.reservations.platform.auth.service.AuthServiceImpl;
import com.udea.reservations.platform.services.controller.ServiceController;
import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.service.ServiceOfferingService;
import com.udea.reservations.platform.users.config.PasswordConfig;
import com.udea.reservations.platform.users.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {ServiceController.class, AuthController.class})
@Import({
    SecurityConfiguration.class,
    SecurityErrorResponseWriter.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class
})
class ServiceAuthorizationIntegrationTest {

    private static final String CREATE_PATH = "/api/v1/services";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServiceOfferingService service;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        when(service.createForProvider(any(), any())).thenReturn(
            new ServiceResponse(15, "Consulta", "Consulta inicial", 45)
        );
    }

    @Test
    void providerCanCreateServiceAndOwnershipComesFromSession() throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);

        mockMvc.perform(post(CREATE_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Consulta","descripcion":"Consulta inicial",
                     "duracionMinutos":45,"providerId":999,"userId":888,
                     "role":"responsable"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(15))
            .andExpect(jsonPath("$.data.nombre").value("Consulta"))
            .andExpect(jsonPath("$.data.duracionMinutos").value(45))
            .andExpect(jsonPath("$.data.providerId").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(service).createForProvider(
            org.mockito.ArgumentMatchers.eq(42),
            org.mockito.ArgumentMatchers.any(ServiceCreateRequest.class)
        );
    }

    @Test
    void anonymousRequestWithValidCsrfReturnsAuthenticationRequired()
        throws Exception {
        CsrfSession csrf = csrfSession();

        mockMvc.perform(post(CREATE_PATH)
                .session(csrf.session())
                .cookie(csrf.cookie())
                .header("X-CSRF-TOKEN", csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void nonProviderRolesAreDenied() throws Exception {
        for (String role : new String[] {"cliente", "responsable"}) {
            AuthenticatedSession session = authenticatedSession(role, 42);

            mockMvc.perform(post(CREATE_PATH)
                    .session(session.session())
                    .cookie(session.cookie())
                    .header("X-CSRF-TOKEN", session.csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingCsrfIsRejectedBeforeAuthorization() throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);

        mockMvc.perform(post(CREATE_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingBlankAndOversizedNames() throws Exception {
        assertValidationFailure("{\"descripcion\":\"desc\",\"duracionMinutos\":10}");
        assertValidationFailure("{\"nombre\":\"\",\"descripcion\":\"desc\",\"duracionMinutos\":10}");
        assertValidationFailure("{\"nombre\":\"   \",\"descripcion\":\"desc\",\"duracionMinutos\":10}");
        assertValidationFailure("{\"nombre\":\"" + "N".repeat(151)
            + "\",\"descripcion\":\"desc\",\"duracionMinutos\":10}");
    }

    @Test
    void rejectsMissingAndBlankDescriptions() throws Exception {
        assertValidationFailure("{\"nombre\":\"Consulta\",\"duracionMinutos\":10}");
        assertValidationFailure("{\"nombre\":\"Consulta\",\"descripcion\":\"\","
            + "\"duracionMinutos\":10}");
        assertValidationFailure("{\"nombre\":\"Consulta\",\"descripcion\":\"   \","
            + "\"duracionMinutos\":10}");
    }

    @Test
    void rejectsMissingZeroAndNegativeDurations() throws Exception {
        assertValidationFailure("{\"nombre\":\"Consulta\",\"descripcion\":\"desc\"}");
        assertValidationFailure("{\"nombre\":\"Consulta\",\"descripcion\":\"desc\","
            + "\"duracionMinutos\":0}");
        assertValidationFailure("{\"nombre\":\"Consulta\",\"descripcion\":\"desc\","
            + "\"duracionMinutos\":-1}");
    }

    private void assertValidationFailure(String body) throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);

        mockMvc.perform(post(CREATE_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(service);
    }

    private AuthenticatedSession authenticatedSession(String role, int userId)
        throws Exception {
        CsrfSession csrf = csrfSession();
        UserPrincipal principal = new UserPrincipal(
            userId,
            "Test Provider",
            "test@example.com",
            "not-used",
            role
        );
        SecurityContext context = new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
            )
        );
        csrf.session().setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return new AuthenticatedSession(
            csrf.session(),
            csrf.cookie(),
            csrf.token()
        );
    }

    private CsrfSession csrfSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = objectMapper.readTree(
            result.getResponse().getContentAsString()
        );
        MockHttpSession session =
            (MockHttpSession) result.getRequest().getSession(false);
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        return new CsrfSession(session, cookie, body.get("token").asString());
    }

    private static String validBody() {
        return """
            {"nombre":"Consulta","descripcion":"Consulta inicial","duracionMinutos":45}
            """;
    }

    private record CsrfSession(
        MockHttpSession session,
        Cookie cookie,
        String token
    ) {}

    private record AuthenticatedSession(
        MockHttpSession session,
        Cookie cookie,
        String csrfToken
    ) {}
}
