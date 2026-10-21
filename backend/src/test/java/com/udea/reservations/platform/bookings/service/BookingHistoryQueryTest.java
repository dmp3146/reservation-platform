package com.udea.reservations.platform.bookings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.bookings.dto.BookingDetailResponse;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.exception.BookingNotFoundException;
import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.users.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingHistoryQueryTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void returnsAllStoredStatesInRepositoryOrder() {
        List<Booking> bookings = List.of(
            booking(57, "completada", LocalTime.of(12, 0)),
            booking(56, "cancelada", LocalTime.of(10, 0)),
            booking(55, "activa", LocalTime.of(9, 0))
        );
        when(bookingRepository.findHistoryByUserIdWithDetails(42))
            .thenReturn(bookings);

        List<BookingResponse> responses =
            bookingService.findHistoryForCustomer(42);

        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).status()).isEqualTo("completada");
        assertThat(responses.get(1).status()).isEqualTo("cancelada");
        assertThat(responses.get(2).status()).isEqualTo("activa");
        assertThat(responses.get(0).bookingId()).isEqualTo(57);
        assertThat(responses.get(1).bookingId()).isEqualTo(56);
        assertThat(responses.get(2).bookingId()).isEqualTo(55);
        verify(bookingRepository).findHistoryByUserIdWithDetails(42);
    }

    @Test
    void returnsEmptyHistoryWhenCustomerHasNoBookings() {
        when(bookingRepository.findHistoryByUserIdWithDetails(42))
            .thenReturn(List.of());

        assertThat(bookingService.findHistoryForCustomer(42)).isEmpty();

        verify(bookingRepository).findHistoryByUserIdWithDetails(42);
    }

    @Test
    void mapsOwnedDetailWithBasicCustomerAndServiceInformation() {
        Booking booking = booking(55, "cancelada", LocalTime.of(9, 30));
        User customer = new User();
        customer.setId(42);
        customer.setName("Ana Cliente");
        booking.setUser(customer);
        when(bookingRepository.findOwnedByIdWithDetails(55, 42))
            .thenReturn(Optional.of(booking));

        BookingDetailResponse response = bookingService.findOwnedBooking(42, 55);

        assertThat(response).isEqualTo(new BookingDetailResponse(
            55,
            15,
            "Consulta",
            "Café Central",
            "Ana Cliente",
            LocalDate.of(2030, 1, 2),
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            "cancelada"
        ));
        verify(bookingRepository).findOwnedByIdWithDetails(55, 42);
    }

    @Test
    void returnsSameNotFoundErrorForMissingOrNotOwnedBooking() {
        when(bookingRepository.findOwnedByIdWithDetails(55, 42))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findOwnedBooking(42, 55))
            .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void mapsLegacyHistoryWithoutUnavailableRelationsOrInventedEndTime() {
        Booking legacy = new Booking();
        legacy.setId(58);
        legacy.setLegacyScheduleId(99);
        legacy.setDate(LocalDate.of(2029, 12, 31));
        legacy.setStartTime(LocalTime.of(15, 0));
        legacy.setStatus("completada");
        when(bookingRepository.findHistoryByUserIdWithDetails(42))
            .thenReturn(List.of(legacy));

        assertThat(bookingService.findHistoryForCustomer(42))
            .containsExactly(new BookingResponse(
                58,
                null,
                null,
                null,
                LocalDate.of(2029, 12, 31),
                LocalTime.of(15, 0),
                null,
                "completada"
            ));
    }

    private static Booking booking(int id, String status, LocalTime startTime) {
        Provider provider = new Provider();
        provider.setBusinessName("Café Central");

        ServiceOffering service = new ServiceOffering();
        service.setId(15);
        service.setName("Consulta");
        service.setDurationMinutes(30);
        service.setProvider(provider);

        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);

        Booking booking = new Booking();
        booking.setId(id);
        booking.setScheduleBlock(block);
        booking.setDate(LocalDate.of(2030, 1, 2));
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(30));
        booking.setStatus(status);
        return booking;
    }
}
