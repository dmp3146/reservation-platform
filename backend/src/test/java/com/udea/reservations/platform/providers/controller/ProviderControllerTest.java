package com.udea.reservations.platform.providers.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.common.web.GlobalExceptionHandler;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.service.ProviderService;
import com.udea.reservations.platform.users.exception.EmailAlreadyRegisteredException;
import com.udea.reservations.platform.users.exception.PasswordMismatchException;
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

class ProviderControllerTest {

    private final FakeProviderService providerService =
        new FakeProviderService();
    private MockMvc mockMvc;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(
            new ProviderController(providerService)
        )
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(new SpringValidatorAdapter(validator))
            .build();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void registerReturns201AndOnlyPublicProviderFields() throws Exception {
        providerService.response = new ProviderRegistrationResponse(
            21,
            "Ana Pérez",
            "Café Ana",
            "ana@example.com",
            null
        );

        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    " ANA@EXAMPLE.COM ",
                    "clave-segura-1",
                    "clave-segura-1",
                    "Café Ana",
                    null
                ))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.providerId").value(21))
            .andExpect(jsonPath("$.data.userName").value("Ana Pérez"))
            .andExpect(jsonPath("$.data.businessName").value("Café Ana"))
            .andExpect(jsonPath("$.data.email").value("ana@example.com"))
            .andExpect(jsonPath("$.data.phone").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.confirmPassword").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("clave-segura-1")
            )));

        org.assertj.core.api.Assertions.assertThat(providerService.request.email())
            .isEqualTo("ana@example.com");
    }

    @Test
    void registerReturnsValidationErrorsForRequiredAndInvalidFields()
        throws Exception {
        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody("", "no-es-un-correo", "corta", "", "", null))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("businessName")
            ));

        org.assertj.core.api.Assertions.assertThat(providerService.request)
            .isNull();
    }

    @Test
    void registerRejectsBusinessNameAndPhoneLongerThanSchemaLimits()
        throws Exception {
        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "clave-segura-1",
                    "N".repeat(151),
                    "3".repeat(31)
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("businessName")
            ))
            .andExpect(content().string(
                org.hamcrest.Matchers.containsString("phone")
            ));

        org.assertj.core.api.Assertions.assertThat(providerService.request)
            .isNull();
    }

    @Test
    void registerAcceptsBusinessNameAndPhoneAtSchemaLimits() throws Exception {
        String businessName = "N".repeat(150);
        String phone = "3".repeat(30);
        providerService.response = new ProviderRegistrationResponse(
            21,
            "Ana Pérez",
            businessName,
            "ana@example.com",
            phone
        );

        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "clave-segura-1",
                    businessName,
                    phone
                ))
        ).andExpect(status().isCreated());
    }

    @Test
    void registerRejectsPasswordLongerThan72Utf8Bytes() throws Exception {
        String password = "ñ".repeat(37);

        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    password,
                    password,
                    "Café Ana",
                    null
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString(password)
            )));

        org.assertj.core.api.Assertions.assertThat(providerService.request)
            .isNull();
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        providerService.failure = new EmailAlreadyRegisteredException();

        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "clave-segura-1",
                    "Café Ana",
                    null
                ))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void registerReturns400ForMismatchedPasswords() throws Exception {
        providerService.failure = new PasswordMismatchException();

        mockMvc.perform(
            post("/api/v1/providers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(
                    "Ana Pérez",
                    "ana@example.com",
                    "clave-segura-1",
                    "otra-clave-2",
                    "Café Ana",
                    null
                ))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"));
    }

    private static String validBody(
        String name,
        String email,
        String password,
        String confirmPassword,
        String businessName,
        String phone
    ) {
        String phoneProperty = phone == null
            ? ""
            : ",\n  \"phone\": \"%s\"".formatted(phone);
        return """
            {
              "name": "%s",
              "email": "%s",
              "password": "%s",
              "confirmPassword": "%s",
              "businessName": "%s"%s
            }
            """.formatted(
            name,
            email,
            password,
            confirmPassword,
            businessName,
            phoneProperty
        );
    }

    private static final class FakeProviderService implements ProviderService {

        private ProviderRegistrationRequest request;
        private ProviderRegistrationResponse response;
        private RuntimeException failure;

        @Override
        public ProviderRegistrationResponse registerProvider(
            ProviderRegistrationRequest registrationRequest
        ) {
            request = Objects.requireNonNull(registrationRequest);
            if (failure != null) {
                throw failure;
            }
            return Objects.requireNonNull(response);
        }
    }
}
