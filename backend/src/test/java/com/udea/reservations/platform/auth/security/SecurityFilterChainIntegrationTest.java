package com.udea.reservations.platform.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.auth.controller.AuthController;
import com.udea.reservations.platform.auth.service.AuthServiceImpl;
import com.udea.reservations.platform.providers.controller.ProviderController;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.service.ProviderService;
import com.udea.reservations.platform.users.config.PasswordConfig;
import com.udea.reservations.platform.users.controller.UserController;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import com.udea.reservations.platform.users.service.UserService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = {
    AuthController.class,
    UserController.class,
    ProviderController.class
})
@Import({
    SecurityConfiguration.class,
    DatabaseUserDetailsService.class,
    AuthServiceImpl.class,
    PasswordConfig.class,
    SecurityErrorResponseWriter.class,
    SecurityFilterChainIntegrationTest.ProtectedProbeController.class
})
class SecurityFilterChainIntegrationTest {

    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = "correct-password";
    private static final String LOGIN_BODY =
        "{\"email\":\" ANA@EXAMPLE.COM \",\"password\":\"correct-password\","
            + "\"role\":\"responsable\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProviderService providerService;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(12);
        user.setName("Ana Pérez");
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole("cliente");
        when(userRepository.findByEmailIgnoreCase(EMAIL))
            .thenReturn(Optional.of(user));
        when(userService.register(any())).thenReturn(
            new UserResponse(12, "Ana Pérez", EMAIL, "cliente")
        );
        when(providerService.registerProvider(any())).thenReturn(
            new ProviderRegistrationResponse(
                21,
                "Ana Pérez",
                "Café Ana",
                EMAIL,
                null
            )
        );
    }

    @Test
    void csrfEndpointReturnsTokenHeaderAndParameterNames() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.parameterName").value("_csrf"))
            .andReturn();

        assertThat(result.getRequest().getSession(false)).isNotNull();
    }

    @Test
    void sessionCookieUsesHttpOnlySecureAndSameSiteLax() throws Exception {
        ServletContext servletContext = mock(ServletContext.class);
        SessionCookieConfig cookieConfig = mock(SessionCookieConfig.class);
        when(servletContext.getSessionCookieConfig()).thenReturn(cookieConfig);

        new SecurityConfiguration()
            .sessionCookieConfiguration()
            .onStartup(servletContext);

        verify(cookieConfig).setHttpOnly(true);
        verify(cookieConfig).setSecure(true);
        verify(cookieConfig).setAttribute("SameSite", "Lax");
    }

    @Test
    void loginWithoutCsrfReturnsIndividualSecurityError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.message").isNotEmpty())
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void loginWithCsrfPersistsAuthenticationRotatesSessionAndRenewsToken()
        throws Exception {
        TokenSession initial = fetchToken();
        String oldSessionId = initial.session().getId();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                .session(initial.session())
                .cookie(new Cookie("JSESSIONID", oldSessionId))
                .header("X-CSRF-TOKEN", initial.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(12))
            .andExpect(jsonPath("$.data.name").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.email").value(EMAIL))
            .andExpect(jsonPath("$.data.role").value("cliente"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andReturn();

        MockHttpSession authenticatedSession =
            (MockHttpSession) login.getRequest().getSession(false);
        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId()).isNotEqualTo(oldSessionId);

        mockMvc.perform(get("/api/test/protected")
                .session(authenticatedSession)
                .cookie(new Cookie("JSESSIONID", authenticatedSession.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(EMAIL));

        mockMvc.perform(get("/api/test/protected"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                .session(authenticatedSession)
                .cookie(new Cookie("JSESSIONID", authenticatedSession.getId()))
                .header("X-CSRF-TOKEN", initial.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        TokenSession renewed = fetchToken(authenticatedSession);
        assertThat(renewed.token()).isNotEqualTo(initial.token());
    }

    @Test
    void missingUserAndWrongPasswordHaveIdenticalPublicErrors()
        throws Exception {
        String missingUserBody = loginAndReadError(
            "{\"email\":\"missing@example.com\",\"password\":\"correct-password\"}"
        );
        String wrongPasswordBody = loginAndReadError(
            "{\"email\":\"ana@example.com\",\"password\":\"wrong-password\"}"
        );

        assertThat(objectMapper.readTree(missingUserBody).get("errorCode").asString())
            .isEqualTo("INVALID_CREDENTIALS");
        assertThat(objectMapper.readTree(missingUserBody).get("message").asString())
            .isEqualTo("El correo electrónico o la contraseña son incorrectos.");
        assertThat(objectMapper.readTree(missingUserBody).get("message").asString())
            .isEqualTo(objectMapper.readTree(wrongPasswordBody).get("message").asString());
    }

    @Test
    void loginValidatesRequiredFields() throws Exception {
        TokenSession token = fetchToken();

        mockMvc.perform(post("/api/v1/auth/login")
                .session(token.session())
                .header("X-CSRF-TOKEN", token.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void userRegistrationRemainsPublicButRequiresValidCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRegistrationBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        TokenSession token = fetchToken();
        mockMvc.perform(post("/api/v1/users")
                .session(token.session())
                .header("X-CSRF-TOKEN", token.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(userRegistrationBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.rol").value("cliente"));
    }

    @Test
    void providerRegistrationRemainsPublicButRequiresValidCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(providerRegistrationBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        TokenSession token = fetchToken();
        mockMvc.perform(post("/api/v1/providers")
                .session(token.session())
                .header("X-CSRF-TOKEN", token.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(providerRegistrationBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.businessName").value("Café Ana"));
    }

    private String loginAndReadError(String body) throws Exception {
        TokenSession token = fetchToken();
        return mockMvc.perform(post("/api/v1/auth/login")
                .session(token.session())
                .header("X-CSRF-TOKEN", token.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private TokenSession fetchToken() throws Exception {
        return fetchToken(null);
    }

    private TokenSession fetchToken(MockHttpSession session) throws Exception {
        var request = get("/api/v1/auth/csrf");
        if (session != null) {
            request.session(session).cookie(new Cookie("JSESSIONID", session.getId()));
        }
        MvcResult result = mockMvc.perform(request)
            .andExpect(status().isOk())
            .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TokenSession(
            body.get("token").asString(),
            (MockHttpSession) result.getRequest().getSession(false)
        );
    }

    private static String userRegistrationBody() {
        return """
            {"name":"Ana Pérez","email":"ana@example.com","password":"clave-segura-1",
             "confirmPassword":"clave-segura-1"}
            """;
    }

    private static String providerRegistrationBody() {
        return """
            {"name":"Ana Pérez","email":"ana@example.com","password":"clave-segura-1",
             "confirmPassword":"clave-segura-1","businessName":"Café Ana"}
            """;
    }

    private record TokenSession(String token, MockHttpSession session) {}

    @org.springframework.web.bind.annotation.RestController
    static class ProtectedProbeController {

        @org.springframework.web.bind.annotation.GetMapping("/api/test/protected")
        ProtectedIdentity identity(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            UserPrincipal principal
        ) {
            return new ProtectedIdentity(principal.email());
        }
    }

    private record ProtectedIdentity(String email) {}
}
