package com.udea.reservations.platform.users.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.common.web.GlobalExceptionHandler;
import com.udea.reservations.platform.users.dto.UserRegistrationRequest;
import com.udea.reservations.platform.users.dto.UserResponse;
import com.udea.reservations.platform.users.exception.EmailAlreadyRegisteredException;
import com.udea.reservations.platform.users.exception.PasswordMismatchException;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.service.UserService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

class UserControllerTest {

    private final FakeUserService userService = new FakeUserService();
    private MockMvc mockMvc;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new SpringValidatorAdapter(validator))
            .build();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void registerReturns201AndOnlyPublicUserFields() throws Exception {
        userService.response = new UserResponse(
            12,
            "Ana Pérez",
            "ana@example.com",
            "cliente"
        );

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    " ANA@EXAMPLE.COM ",
                    "clave-segura-1",
                    "clave-segura-1"
                ))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(12))
            .andExpect(jsonPath("$.data.name").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.email").value("ana@example.com"))
            .andExpect(jsonPath("$.data.rol").value("cliente"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("clave-segura-1")
            )));

        org.assertj.core.api.Assertions.assertThat(userService.request.email())
            .isEqualTo("ana@example.com");
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        userService.failure = new EmailAlreadyRegisteredException();

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "clave-segura-1"
                ))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void registerReturns400ForMismatchedPasswords() throws Exception {
        userService.failure = new PasswordMismatchException();

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "otra-clave-2"
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void registerReturnsValidationErrorsForRequiredAndInvalidFields()
        throws Exception {
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody("", "no-es-un-correo", "corta", ""))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("name")
            ))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("email")
            ))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("password")
            ));

        org.assertj.core.api.Assertions.assertThat(userService.request).isNull();
    }

    @Test
    void registerRejectsPasswordLongerThan72Utf8Bytes() throws Exception {
        String password = "ñ".repeat(37);

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    password,
                    password
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString(password)
            )));
    }

    @Test
    void registerAcceptsPasswordAt72Utf8ByteBoundary() throws Exception {
        String password = "ñ".repeat(36);
        userService.response = new UserResponse(
            12,
            "Ana Pérez",
            "ana@example.com",
            "cliente"
        );

        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    password,
                    password
                ))
        ).andExpect(status().isCreated());
    }

    @Test
    void registerRejectsMalformedJsonUsingGlobalErrorContract() throws Exception {
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private static String validBody(
        String name,
        String email,
        String password,
        String confirmPassword
    ) {
        return """
            {
              "name": "%s",
              "email": "%s",
              "password": "%s",
              "confirmPassword": "%s"
            }
            """.formatted(name, email, password, confirmPassword);
    }

    private static final class FakeUserService implements UserService {

        private UserRegistrationRequest request;
        private UserResponse response;
        private RuntimeException failure;

        @Override
        public UserResponse register(UserRegistrationRequest registrationRequest) {
            request = Objects.requireNonNull(registrationRequest);
            if (failure != null) {
                throw failure;
            }
            return Objects.requireNonNull(response);
        }

        @Override
        public User registerProviderAccount(
            UserRegistrationRequest registrationRequest
        ) {
            throw new UnsupportedOperationException(
                "Provider registration is not used by this test."
            );
        }
    }
}
