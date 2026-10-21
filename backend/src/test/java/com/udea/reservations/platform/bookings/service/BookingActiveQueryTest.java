package com.udea.reservations.platform.bookings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.services.model.ServiceOffering;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingActiveQueryTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void mapsActiveBookingsWithProviderDetailsAndPreservesRepositoryOrder() {
        Booking first = booking(
            55,
            LocalDate.of(2030, 1, 2),
            LocalTime.of(9, 30),
            "activa",
            "Consulta",
            "Café Central"
        );
        Booking second = booking(
            56,
            LocalDate.of(2030, 1, 3),
            LocalTime.of(11, 0),
            "activa",
            "Control",
            "Clínica Norte"
        );
        when(bookingRepository.findAllByUserIdAndStatusWithDetails(42, "activa"))
            .thenReturn(List.of(first, second));

        List<BookingResponse> responses =
            bookingService.findActiveForCustomer(42);

        assertThat(responses).containsExactly(
            new BookingResponse(
                55,
                15,
                "Consulta",
                "Café Central",
                LocalDate.of(2030, 1, 2),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0),
                "activa"
            ),
            new BookingResponse(
                56,
                15,
                "Control",
                "Clínica Norte",
                LocalDate.of(2030, 1, 3),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                "activa"
            )
        );
        verify(bookingRepository)
            .findAllByUserIdAndStatusWithDetails(42, "activa");
    }

    @Test
    void returnsEmptyListWhenRepositoryFindsNoActiveBookings() {
        when(bookingRepository.findAllByUserIdAndStatusWithDetails(42, "activa"))
            .thenReturn(List.of());

        assertThat(bookingService.findActiveForCustomer(42)).isEmpty();

        verify(bookingRepository)
            .findAllByUserIdAndStatusWithDetails(42, "activa");
    }

    @Test
    void mapsLegacyBookingWithoutServiceRelationsWithoutFailing() {
        Booking legacyBooking = new Booking();
        legacyBooking.setId(57);
        legacyBooking.setDate(LocalDate.of(2030, 1, 4));
        legacyBooking.setStartTime(LocalTime.of(12, 0));
        legacyBooking.setStatus("activa");
        when(bookingRepository.findAllByUserIdAndStatusWithDetails(42, "activa"))
            .thenReturn(List.of(legacyBooking));

        assertThat(bookingService.findActiveForCustomer(42))
            .containsExactly(new BookingResponse(
                57,
                null,
                null,
                null,
                LocalDate.of(2030, 1, 4),
                LocalTime.of(12, 0),
                null,
                "activa"
            ));
    }

    private static Booking booking(
        int id,
        LocalDate date,
        LocalTime startTime,
        String status,
        String serviceName,
        String providerName
    ) {
        Provider provider = new Provider();
        provider.setBusinessName(providerName);

        ServiceOffering service = new ServiceOffering();
        service.setId(15);
        service.setName(serviceName);
        service.setDurationMinutes(30);
        service.setProvider(provider);

        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);

        Booking booking = new Booking();
        booking.setId(id);
        booking.setScheduleBlock(block);
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setStatus(status);
        return booking;
    }
}
