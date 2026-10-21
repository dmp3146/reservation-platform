package com.udea.reservations.platform.schedules.controller;

import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import com.udea.reservations.platform.schedules.dto.ScheduleCreateRequest;
import com.udea.reservations.platform.schedules.dto.ScheduleResponse;
import com.udea.reservations.platform.schedules.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/providers/me/schedules")
    public ResponseEntity<ApiSuccessResponse<ScheduleResponse>> create(
        @Valid @RequestBody ScheduleCreateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ScheduleResponse response = scheduleService.createForProvider(
            principal.id(),
            request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ApiSuccessResponse<>("Horario disponible creado exitosamente.", response)
        );
    }

    @GetMapping("/services/{serviceId}/schedules")
    public ResponseEntity<ApiSuccessResponse<List<ScheduleResponse>>> findByService(
        @PathVariable Integer serviceId
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Horarios disponibles consultados exitosamente.",
            scheduleService.findByServiceId(serviceId)
        ));
    }
}
