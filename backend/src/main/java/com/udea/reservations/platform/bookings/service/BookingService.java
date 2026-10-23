package com.udea.reservations.platform.bookings.service;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingDetailResponse;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.dto.BookedSlotResponse;
import java.util.List;

public interface BookingService {

    BookingResponse create(Integer authenticatedUserId, BookingCreateRequest request);

    List<BookingResponse> findActiveForCustomer(Integer authenticatedUserId);

    List<BookingResponse> findActiveForProvider(Integer authenticatedUserId);

    List<BookedSlotResponse> findBookedSlotsForService(Integer serviceId);

    BookingResponse cancel(Integer authenticatedUserId, Integer bookingId);

    List<BookingResponse> findHistoryForCustomer(Integer authenticatedUserId);

    BookingDetailResponse findOwnedBooking(
        Integer authenticatedUserId,
        Integer bookingId
    );
}
