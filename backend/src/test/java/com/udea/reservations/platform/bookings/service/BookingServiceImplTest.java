package com.udea.reservations.platform.bookings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.exception.BookingAlreadyCancelledException;
import com.udea.reservations.platform.bookings.exception.BookingConflictException;
import com.udea.reservations.platform.bookings.exception.BookingCompletedCannotBeCancelledException;
import com.udea.reservations.platform.bookings.exception.BookingDateTimeInPastException;
import com.udea.reservations.platform.bookings.exception.BookingNotFoundException;
import com.udea.reservations.platform.bookings.exception.BookingScheduleNotFoundException;
import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Integer BLOCK_ID = 31;
    private static final Integer USER_ID = 42;
    private static final LocalDate BOOKING_DATE = LocalDate.of(2030, 1, 2);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private UserRepository userRepository;

    private BookingServiceImpl bookingService;
    private ScheduleBlock scheduleBlock;
    private ServiceOffering service;
    private User user;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
            bookingRepository,
            scheduleBlockRepository,
            userRepository,
            Clock.fixed(
                Instant.parse("2030-01-01T08:00:00Z"),
                ZoneOffset.UTC
            )
        );

        user = new User();
        user.setId(USER_ID);

        Provider provider = new Provider();
        provider.setId(7);
        provider.setBusinessName("Café Central");

        service = new ServiceOffering();
        service.setId(15);
        service.setName("Consulta");
        service.setDurationMinutes(30);
        service.setActive(true);
        service.setProvider(provider);

        scheduleBlock = new ScheduleBlock();
        scheduleBlock.setId(BLOCK_ID);
        scheduleBlock.setService(service);
        scheduleBlock.setDate(BOOKING_DATE);
        scheduleBlock.setStartTime(LocalTime.of(9, 0));
        scheduleBlock.setEndTime(LocalTime.of(12, 0));
        lenient().when(scheduleBlockRepository.findByIdForUpdate(BLOCK_ID))
            .thenReturn(Optional.of(scheduleBlock));
    }

    @Test
    void createsBookingForAuthenticatedUserAndReturnsServiceDetails() {
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(bookingRepository.existsActiveOverlap(
            BLOCK_ID,
            BOOKING_DATE,
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
        )).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class)))
            .thenAnswer(invocation -> {
                Booking booking = invocation.getArgument(0);
                booking.setId(55);
                return booking;
            });

        BookingResponse response = bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(9, 30))
        );

        assertThat(response).isEqualTo(new BookingResponse(
            55,
            15,
            "Consulta",
            "Café Central",
            BOOKING_DATE,
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            "activa"
        ));
        ArgumentCaptor<Booking> bookingCaptor =
            ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getUser()).isSameAs(user);
        assertThat(bookingCaptor.getValue().getScheduleBlock())
            .isSameAs(scheduleBlock);
    }

    @Test
    void rejectsAnOverlappingActiveBookingWithoutPersisting() {
        when(bookingRepository.existsActiveOverlap(
            BLOCK_ID,
            BOOKING_DATE,
            LocalTime.of(9, 30),
            LocalTime.of(10, 0)
        )).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(9, 30))
        )).isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInactiveServiceWithoutPersisting() {
        service.setActive(false);

        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(9, 30))
        )).isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStartOutsideAvailabilityBlockWithoutPersisting() {
        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(8, 30))
        )).isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsServiceDurationThatCrossesDayBoundaryWithoutPersisting() {
        service.setDurationMinutes(1_500);

        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(9, 30))
        )).isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPastDateTimeWithoutPersisting() {
        scheduleBlock.setDate(LocalDate.of(2030, 1, 1));

        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(7, 30))
        )).isInstanceOf(BookingDateTimeInPastException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportsMissingAvailabilityBlockWithoutPersisting() {
        when(scheduleBlockRepository.findByIdForUpdate(BLOCK_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(
            USER_ID,
            new BookingCreateRequest(BLOCK_ID, LocalTime.of(9, 30))
        )).isInstanceOf(BookingScheduleNotFoundException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelsOwnedActiveBookingAndPersistsTheStatusChange() {
        Booking booking = booking("activa", user);
        when(bookingRepository.findByIdForUpdate(55))
            .thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        BookingResponse response = bookingService.cancel(USER_ID, 55);

        assertThat(booking.getStatus()).isEqualTo("cancelada");
        assertThat(response).isEqualTo(new BookingResponse(
            55,
            15,
            "Consulta",
            "Café Central",
            BOOKING_DATE,
            LocalTime.of(9, 30),
            LocalTime.of(10, 0),
            "cancelada"
        ));
        verify(bookingRepository).saveAndFlush(booking);
    }

    @Test
    void rejectsCancellingAlreadyCancelledBookingWithoutSavingAgain() {
        Booking booking = booking("cancelada", user);
        when(bookingRepository.findByIdForUpdate(55))
            .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 55))
            .isInstanceOf(BookingAlreadyCancelledException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsCancellingCompletedBookingWithoutChangingItsState() {
        Booking booking = booking("completada", user);
        when(bookingRepository.findByIdForUpdate(55))
            .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 55))
            .isInstanceOf(BookingCompletedCannotBeCancelledException.class);

        assertThat(booking.getStatus()).isEqualTo("completada");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void hidesBookingOwnedByAnotherCustomer() {
        User anotherCustomer = new User();
        anotherCustomer.setId(84);
        Booking booking = booking("activa", anotherCustomer);
        when(bookingRepository.findByIdForUpdate(55))
            .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 55))
            .isInstanceOf(BookingNotFoundException.class);

        assertThat(booking.getStatus()).isEqualTo("activa");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportsMissingBookingWithoutSaving() {
        when(bookingRepository.findByIdForUpdate(55))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(USER_ID, 55))
            .isInstanceOf(BookingNotFoundException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    private Booking booking(String status, User bookingUser) {
        Booking booking = new Booking();
        booking.setId(55);
        booking.setUser(bookingUser);
        booking.setScheduleBlock(scheduleBlock);
        booking.setDate(BOOKING_DATE);
        booking.setStartTime(LocalTime.of(9, 30));
        booking.setEndTime(LocalTime.of(10, 0));
        booking.setStatus(status);
        return booking;
    }
}
