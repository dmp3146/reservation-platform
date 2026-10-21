package com.udea.reservations.platform.bookings.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.udea.reservations.platform.bookings.controller.BookingController;
import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.service.BookingService;
import com.udea.reservations.platform.common.web.GlobalExceptionHandler;
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

@WebMvcTest(controllers = {BookingController.class, AuthController.class})
@Import({
    SecurityConfiguration.class,
    SecurityErrorResponseWriter.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class,
    GlobalExceptionHandler.class
})
class BookingAuthorizationIntegrationTest {

    private static final String BOOKING_PATH = "/api/v1/bookings";
    private static final String ACTIVE_BOOKINGS_PATH =
        "/api/v1/bookings/active";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void clientCanCreateBookingAndIdentityComesFromSession() throws Exception {
        when(bookingService.create(eq(42), any(BookingCreateRequest.class)))
            .thenReturn(new BookingResponse(
                55,
                15,
                "Consulta",
                "Café Central",
                LocalDate.of(2030, 1, 2),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                "activa"
            ));
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(post(BOOKING_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"scheduleBlockId":31,"startTime":"09:30","userId":999}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.bookingId").value(55))
            .andExpect(jsonPath("$.data.serviceName").value("Consulta"))
            .andExpect(jsonPath("$.data.date").value("2030-01-02"))
            .andExpect(jsonPath("$.data.startTime").value("09:30:00"))
            .andExpect(jsonPath("$.data.endTime").value("10:00:00"))
            .andExpect(jsonPath("$.data.userId").doesNotExist());

        verify(bookingService).create(eq(42), any(BookingCreateRequest.class));
    }

    @Test
    void clientCanReadOnlyTheirActiveBookingsAndUserHeaderIsIgnored()
        throws Exception {
        when(bookingService.findActiveForCustomer(42)).thenReturn(List.of(
            new BookingResponse(
                55,
                15,
                "Consulta",
                "Café Central",
                LocalDate.of(2030, 1, 2),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                "activa"
            ),
            new BookingResponse(
                56,
                16,
                "Control",
                "Clínica Norte",
                LocalDate.of(2030, 1, 3),
                LocalTime.of(11, 0),
                LocalTime.of(11, 45),
                "activa"
            )
        ));
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(get(ACTIVE_BOOKINGS_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-User-Id", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].bookingId").value(55))
            .andExpect(jsonPath("$.data[0].serviceName").value("Consulta"))
            .andExpect(jsonPath("$.data[0].providerName").value("Café Central"))
            .andExpect(jsonPath("$.data[0].date").value("2030-01-02"))
            .andExpect(jsonPath("$.data[0].startTime").value("09:30:00"))
            .andExpect(jsonPath("$.data[0].endTime").value("10:00:00"))
            .andExpect(jsonPath("$.data[0].status").value("activa"))
            .andExpect(jsonPath("$.data[1].bookingId").value(56));

        verify(bookingService).findActiveForCustomer(42);
    }

    @Test
    void clientWithNoActiveBookingsReceivesAnEmptyCollection() throws Exception {
        when(bookingService.findActiveForCustomer(42)).thenReturn(List.of());
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(get(ACTIVE_BOOKINGS_PATH)
                .session(session.session())
                .cookie(session.cookie()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));

        verify(bookingService).findActiveForCustomer(42);
    }

    @Test
    void anonymousRequestRequiresAuthentication() throws Exception {
        CsrfSession session = csrfSession();

        mockMvc.perform(get(ACTIVE_BOOKINGS_PATH)
                .session(session.session())
                .cookie(session.cookie()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post(BOOKING_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(bookingService);
    }

    @Test
    void nonClientRolesAreDenied() throws Exception {
        for (String role : new String[] {"proveedor", "responsable"}) {
            AuthenticatedSession session = authenticatedSession(role, 42);

            mockMvc.perform(post(BOOKING_PATH)
                    .session(session.session())
                    .cookie(session.cookie())
                    .header("X-CSRF-TOKEN", session.csrfToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

            mockMvc.perform(get(ACTIVE_BOOKINGS_PATH)
                    .session(session.session())
                    .cookie(session.cookie())
                    .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        }

        verifyNoInteractions(bookingService);
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(post(BOOKING_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(bookingService);
    }

    @Test
    void invalidRequestUsesStandardValidationError() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(post(BOOKING_PATH)
                .session(session.session())
                .cookie(session.cookie())
                .header("X-CSRF-TOKEN", session.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduleBlockId\":0,\"startTime\":null}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(bookingService);
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
            {"scheduleBlockId":31,"startTime":"09:30"}
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
