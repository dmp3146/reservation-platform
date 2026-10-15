
package com.udea.reservations.platform.common.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservations.platform.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Invalid fields return 400")
    void shouldReturnBadRequestWhenFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("Malformed JSON returns 400")
    void shouldReturnBadRequestWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Business rule returns its HTTP status and error code")
    void shouldReturnBusinessRuleStatusAndCode() throws Exception {
        mockMvc.perform(get("/fake/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    @DisplayName("Unsupported method returns 405")
    void shouldReturnMethodNotAllowedWhenMethodIsUnsupported() throws Exception {
        mockMvc.perform(get("/fake/validate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Unexpected errors do not expose internal details")
    void shouldHideInternalDetailsWhenUnexpectedErrorOccurs() throws Exception {
        mockMvc.perform(get("/fake/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("detalle interno secreto"))));
    }

    @RestController
    @RequestMapping("/fake")
    public static class FakeController {

        record Body(@NotBlank(message = "es obligatorio") String name) {}

        @PostMapping("/validate")
        public String validate(@Valid @RequestBody Body body) {
            return "ok";
        }

        @GetMapping("/business")
        public String business() {
            throw new BusinessRuleException(
                    "EMAIL_TAKEN",
                    "El correo ya está registrado.",
                    409
            );
        }

        @GetMapping("/boom")
        public String boom() {
            throw new IllegalStateException("detalle interno secreto");
        }
    }
}
