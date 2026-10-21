package com.udea.reservations.platform.bookings.controller;

import com.udea.reservations.platform.auth.security.UserPrincipal;
import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingDetailResponse;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.service.BookingService;
import com.udea.reservations.platform.common.dto.ApiSuccessResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<BookingResponse>> create(
        @Valid @RequestBody BookingCreateRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        BookingResponse response = bookingService.create(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new ApiSuccessResponse<>("Reserva creada exitosamente.", response)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<ApiSuccessResponse<List<BookingResponse>>> findActive(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Reservas activas consultadas exitosamente.",
            bookingService.findActiveForCustomer(principal.id())
        ));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiSuccessResponse<BookingResponse>> cancel(
        @PathVariable Integer bookingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Reserva cancelada exitosamente.",
            bookingService.cancel(principal.id(), bookingId)
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiSuccessResponse<List<BookingResponse>>> findHistory(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Historial de reservas consultado exitosamente.",
            bookingService.findHistoryForCustomer(principal.id())
        ));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiSuccessResponse<BookingDetailResponse>> findById(
        @PathVariable Integer bookingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Detalle de reserva consultado exitosamente.",
            bookingService.findOwnedBooking(principal.id(), bookingId)
        ));
    }
}
