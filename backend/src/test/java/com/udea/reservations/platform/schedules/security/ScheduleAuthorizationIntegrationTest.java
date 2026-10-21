package com.udea.reservations.platform.schedules.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.auth.controller.AuthController;
import com.udea.reservations.platform.auth.security.DatabaseUserDetailsService;
import com.udea.reservations.platform.auth.security.SecurityConfiguration;
import com.udea.reservations.platform.auth.security.SecurityErrorResponseWriter;
import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.auth.service.AuthServiceImpl;
import com.udea.reservations.platform.schedules.controller.ScheduleController;
import com.udea.reservations.platform.schedules.dto.ScheduleCreateRequest;
import com.udea.reservations.platform.schedules.dto.ScheduleResponse;
import com.udea.reservations.platform.schedules.exception.ScheduleOverlapException;
import com.udea.reservations.platform.schedules.service.ScheduleService;
import com.udea.reservations.platform.users.config.PasswordConfig;
import com.udea.reservations.platform.users.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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

@WebMvcTest(controllers = {ScheduleController.class, AuthController.class})
@Import({
    SecurityConfiguration.class,
    SecurityErrorResponseWriter.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class
})
class ScheduleAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void providerCanCreateScheduleWithCsrf() throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);
        when(scheduleService.createForProvider(any(), any()))
            .thenReturn(new ScheduleResponse(
                31,
                15,
                LocalDate.parse("2026-10-22"),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
            ));

        mockMvc.perform(post("/api/v1/providers/me/schedules")
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"serviceId":15,"fecha":"2026-10-22",
                     "horaInicio":"09:00","horaFin":"10:00",
                     "providerId":999,"userId":888}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(31))
            .andExpect(jsonPath("$.data.serviceId").value(15))
            .andExpect(jsonPath("$.data.providerId").doesNotExist());

        verify(scheduleService).createForProvider(
            org.mockito.ArgumentMatchers.eq(42),
            any(ScheduleCreateRequest.class)
        );
    }

    @Test
    void nonProviderCannotCreateSchedule() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(post("/api/v1/providers/me/schedules")
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(scheduleService);
    }

    @Test
    void anonymousCreationWithValidCsrfRequiresAuthentication() throws Exception {
        CsrfSession session = csrfSession();

        mockMvc.perform(post("/api/v1/providers/me/schedules")
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void invalidCsrfIsRejected() throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);

        mockMvc.perform(post("/api/v1/providers/me/schedules")
                .session(session.session())
                .cookie(session.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(scheduleService);
    }

    @Test
    void authenticatedUserCanReadServiceSchedules() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);
        when(scheduleService.findByServiceId(15)).thenReturn(List.of(
            new ScheduleResponse(
                31,
                15,
                LocalDate.parse("2026-10-22"),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
            )
        ));

        mockMvc.perform(get("/api/v1/services/15/schedules")
                .session(session.session())
                .cookie(session.cookie()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].serviceId").value(15))
            .andExpect(jsonPath("$.data[0].fecha").value("2026-10-22"));

        verify(scheduleService).findByServiceId(15);
    }

    @Test
    void anonymousUserCannotReadServiceSchedules() throws Exception {
        mockMvc.perform(get("/api/v1/services/15/schedules"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(scheduleService);
    }

    @Test
    void businessConflictUsesSharedErrorContract() throws Exception {
        AuthenticatedSession session = authenticatedSession("proveedor", 42);
        when(scheduleService.createForProvider(any(), any()))
            .thenThrow(new ScheduleOverlapException());

        mockMvc.perform(post("/api/v1/providers/me/schedules")
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("SCHEDULE_OVERLAP"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private AuthenticatedSession authenticatedSession(String role, int userId)
        throws Exception {
        CsrfSession csrf = csrfSession();
        UserPrincipal principal = new UserPrincipal(
            userId,
            "Test User",
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
        return new CsrfSession(
            session,
            new Cookie("JSESSIONID", session.getId()),
            body.get("token").asString()
        );
    }

    private static String validBody() {
        return """
            {"serviceId":15,"fecha":"2026-10-22",
             "horaInicio":"09:00","horaFin":"10:00"}
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
