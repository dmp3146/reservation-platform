package com.udea.reservations.platform.providers.security;

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
import com.udea.reservations.platform.providers.controller.ProviderController;
import com.udea.reservations.platform.providers.dto.ProviderPageResponse;
import com.udea.reservations.platform.providers.dto.ProviderSummaryResponse;
import com.udea.reservations.platform.providers.service.ProviderService;
import com.udea.reservations.platform.users.config.PasswordConfig;
import com.udea.reservations.platform.users.repository.UserRepository;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = {ProviderController.class, AuthController.class})
@Import({
    SecurityConfiguration.class,
    SecurityErrorResponseWriter.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class
})
class ProviderDirectoryAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderService providerService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void authenticatedClientCanListProviders() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);
        when(providerService.findProviders(0, 20)).thenReturn(
            new ProviderPageResponse(
                List.of(new ProviderSummaryResponse(7, "Café Ana")),
                0,
                20,
                1,
                1
            )
        );

        mockMvc.perform(get("/api/v1/providers")
                .session(session.session())
                .cookie(session.cookie()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].providerId").value(7));
    }

    @Test
    void anonymousUserCannotListProviders() throws Exception {
        mockMvc.perform(get("/api/v1/providers"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(providerService);
    }

    @Test
    void providerListRejectsPageSizeAboveMaximum() throws Exception {
        AuthenticatedSession session = authenticatedSession("cliente", 42);

        mockMvc.perform(get("/api/v1/providers")
                .param("size", "101")
                .session(session.session())
                .cookie(session.cookie()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(providerService);
    }

    private AuthenticatedSession authenticatedSession(String role, int userId)
        throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session =
            (MockHttpSession) result.getRequest().getSession(false);
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
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
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return new AuthenticatedSession(session, cookie);
    }

    private record AuthenticatedSession(
        MockHttpSession session,
        Cookie cookie
    ) {}
}
