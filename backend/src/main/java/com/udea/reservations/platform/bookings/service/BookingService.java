package com.udea.reservations.platform.bookings.service;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import java.util.List;

public interface BookingService {

    BookingResponse create(Integer authenticatedUserId, BookingCreateRequest request);

    List<BookingResponse> findActiveForCustomer(Integer authenticatedUserId);
}
