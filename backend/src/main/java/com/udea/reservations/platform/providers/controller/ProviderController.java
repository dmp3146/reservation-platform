package com.udea.reservations.platform.providers.controller;

import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationRequest;
import com.udea.reservations.platform.providers.dto.ProviderRegistrationResponse;
import com.udea.reservations.platform.providers.dto.ProviderPageResponse;
import com.udea.reservations.platform.providers.service.ProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@Validated
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

    @GetMapping
    public ApiSuccessResponse<ProviderPageResponse> findProviders(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new ApiSuccessResponse<>(
            "Proveedores consultados exitosamente.",
            providerService.findProviders(page, size)
        );
    }
}
