package com.udea.reservations.platform.providers.controller;

import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ProviderRegistrationResponse>> register(
        @Valid @RequestBody ProviderRegistrationRequest registrationRequest
    ) {
        ProviderRegistrationResponse response =
            providerService.registerProvider(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ApiSuccessResponse<>(
                "Registro exitoso. Tu cuenta de proveedor fue creada.",
                response
            )
        );
    }
}
