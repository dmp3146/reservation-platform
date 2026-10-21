package com.udea.reservations.platform.bookings.service;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.dto.BookingResponse;
import com.udea.reservations.platform.bookings.exception.BookingConflictException;
import com.udea.reservations.platform.bookings.exception.BookingDateTimeInPastException;
import com.udea.reservations.platform.bookings.exception.BookingScheduleNotFoundException;
import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";
    private static final String ACTIVE_STATUS = "activa";

    private final BookingRepository bookingRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final UserRepository userRepository;
    private final Clock bookingClock;

    @Override
    @Transactional
    public BookingResponse create(
        Integer authenticatedUserId,
        BookingCreateRequest request
    ) {
        ScheduleBlock block = scheduleBlockRepository
            .findByIdForUpdate(request.scheduleBlockId())
            .orElseThrow(BookingScheduleNotFoundException::new);
        ServiceOffering service = block.getService();

        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new BookingConflictException();
        }

        if (service.getDurationMinutes() >= MINUTES_PER_DAY) {
            throw new BookingConflictException();
        }
        LocalTime endTime = request.startTime()
            .plusMinutes(service.getDurationMinutes());
        if (!endTime.isAfter(request.startTime())) {
            throw new BookingConflictException();
        }

        LocalDateTime bookingStart = LocalDateTime.of(
            block.getDate(),
            request.startTime()
        );
        if (!bookingStart.isAfter(LocalDateTime.now(bookingClock))) {
            throw new BookingDateTimeInPastException();
        }

        if (request.startTime().isBefore(block.getStartTime())
            || endTime.isAfter(block.getEndTime())) {
            throw new BookingConflictException();
        }

        if (bookingRepository.existsActiveOverlap(
            block.getId(),
            block.getDate(),
            request.startTime(),
            endTime
        )) {
            throw new BookingConflictException();
        }

        Booking booking = new Booking();
        booking.setUser(userReference(authenticatedUserId));
        booking.setScheduleBlock(block);
        booking.setDate(block.getDate());
        booking.setStartTime(request.startTime());
        booking.setEndTime(endTime);
        booking.setStatus(ACTIVE_STATUS);

        try {
            return toResponse(bookingRepository.saveAndFlush(booking));
        } catch (DataIntegrityViolationException exception) {
            if (containsUniqueViolation(exception)) {
                throw new BookingConflictException();
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> findActiveForCustomer(
        Integer authenticatedUserId
    ) {
        return bookingRepository
            .findAllByUserIdAndStatusWithDetails(
                authenticatedUserId,
                ACTIVE_STATUS
            )
            .stream()
            .map(BookingServiceImpl::toResponse)
            .toList();
    }

    private User userReference(Integer authenticatedUserId) {
        return userRepository.getReferenceById(
            Objects.requireNonNull(authenticatedUserId)
        );
    }

    private static BookingResponse toResponse(Booking booking) {
        ScheduleBlock block = booking.getScheduleBlock();
        ServiceOffering service = block == null ? null : block.getService();
        return new BookingResponse(
            booking.getId(),
            service == null ? null : service.getId(),
            service == null ? null : service.getName(),
            service == null ? null : service.getProvider().getBusinessName(),
            booking.getDate(),
            booking.getStartTime(),
            booking.getEndTime() == null
                ? (service == null ? null : booking.getStartTime()
                    .plusMinutes(service.getDurationMinutes()))
                : booking.getEndTime(),
            booking.getStatus()
        );
    }

    private static boolean containsUniqueViolation(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
