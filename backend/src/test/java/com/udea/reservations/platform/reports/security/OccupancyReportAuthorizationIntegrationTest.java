package com.udea.reservations.platform.reports.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.auth.controller.AuthController;
import com.udea.reservations.platform.auth.security.DatabaseUserDetailsService;
import com.udea.reservations.platform.auth.security.SecurityConfiguration;
import com.udea.reservations.platform.auth.security.SecurityErrorResponseWriter;
import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.auth.service.AuthServiceImpl;
import com.udea.reservations.platform.common.web.GlobalExceptionHandler;
import com.udea.reservations.platform.reports.controller.OccupancyReportController;
import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import com.udea.reservations.platform.reports.exception.InvalidOccupancyDateRangeException;
import com.udea.reservations.platform.reports.service.OccupancyReportService;
import com.udea.reservations.platform.users.config.PasswordConfig;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    OccupancyReportController.class,
    AuthController.class
})
@Import({
    SecurityConfiguration.class,
    SecurityErrorResponseWriter.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class,
    GlobalExceptionHandler.class
})
class OccupancyReportAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OccupancyReportService reportService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void anonymousUserMustAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/reports/occupancy")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-31"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(reportService);
    }

    @Test
    void clientAndProviderAreDenied() throws Exception {
        for (String role : new String[] {"cliente", "proveedor"}) {
            mockMvc.perform(get("/api/v1/reports/occupancy")
                    .session(sessionFor(role))
                    .param("startDate", "2026-10-01")
                    .param("endDate", "2026-10-31"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
        }

        verifyNoInteractions(reportService);
    }

    @Test
    void responsibleRoleHasManagerAuthorityAndCanReadReport() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        LocalDate endDate = LocalDate.of(2026, 10, 31);
        when(reportService.generate(startDate, endDate)).thenReturn(
            new OccupancyReportResponse(
                startDate,
                endDate,
                2,
                1,
                1,
                0,
                true,
                List.of(),
                List.of(),
                true,
                "Solo reservas heredadas atribuidas a recursos."
            )
        );

        mockMvc.perform(get("/api/v1/reports/occupancy")
                .session(sessionFor("responsable"))
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalBookings").value(2))
            .andExpect(jsonPath("$.data.effectiveBookings").value(1))
            .andExpect(jsonPath("$.data.cancelledBookings").value(1))
            .andExpect(jsonPath("$.data.hasBookings").value(true))
            .andExpect(jsonPath("$.data.resources").isEmpty())
            .andExpect(jsonPath("$.data.resourceMetricsAvailable").value(true));

        verify(reportService).generate(startDate, endDate);
    }

    @Test
    void reversedDatesUseStandardBadRequestResponse() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 10, 31);
        LocalDate endDate = LocalDate.of(2026, 10, 1);
        when(reportService.generate(startDate, endDate))
            .thenThrow(new InvalidOccupancyDateRangeException());

        mockMvc.perform(get("/api/v1/reports/occupancy")
                .session(sessionFor("responsable"))
                .param("startDate", "2026-10-31")
                .param("endDate", "2026-10-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_REPORT_DATE_RANGE"));

        verify(reportService).generate(startDate, endDate);
    }

    @Test
    void malformedOrMissingDatesReturnBadRequestNotServerError()
        throws Exception {
        MockHttpSession session = sessionFor("responsable");
        LocalDate startDate = LocalDate.of(2026, 10, 1);
        when(reportService.generate(startDate, null))
            .thenThrow(new InvalidOccupancyDateRangeException());

        mockMvc.perform(get("/api/v1/reports/occupancy")
                .session(session)
                .param("startDate", "not-a-date")
                .param("endDate", "2026-10-31"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/reports/occupancy")
                .session(session)
                .param("startDate", startDate.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_REPORT_DATE_RANGE"));

        verify(reportService).generate(startDate, null);
    }

    private static MockHttpSession sessionFor(String role) {
        UserPrincipal principal = new UserPrincipal(
            42,
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
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return session;
    }
}
