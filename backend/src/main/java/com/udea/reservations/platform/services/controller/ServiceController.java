package com.udea.reservations.platform.services.controller;

import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.services.dto.ServiceCreateRequest;
import com.udea.reservations.platform.services.dto.ServiceResponse;
import com.udea.reservations.platform.services.service.ServiceOfferingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class ServiceController {

    private final ServiceOfferingService serviceOfferingService;

    public ServiceController(ServiceOfferingService serviceOfferingService) {
        this.serviceOfferingService = serviceOfferingService;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ServiceResponse>> create(
        @Valid @RequestBody ServiceCreateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ServiceResponse response = serviceOfferingService.createForProvider(
            principal.id(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ApiSuccessResponse<>("Servicio creado exitosamente.", response)
        );
    }
}
