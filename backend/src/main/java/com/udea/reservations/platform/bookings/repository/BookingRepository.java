package com.udea.reservations.platform.bookings.repository;

import com.udea.reservations.platform.bookings.model.Booking;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.id = :bookingId")
    Optional<Booking> findByIdForUpdate(
        @Param("bookingId") Integer bookingId
    );

    @Query("""
        select booking
        from Booking booking
        left join fetch booking.scheduleBlock block
        left join fetch block.service offering
        left join fetch offering.provider provider
        where booking.user.id = :userId
          and booking.status = :status
        order by booking.date asc, booking.startTime asc
        """)
    List<Booking> findAllByUserIdAndStatusWithDetails(
        @Param("userId") Integer userId,
        @Param("status") String status
    );

    boolean existsByScheduleBlock_Service_IdAndStatus(
        Integer serviceId,
        String status
    );

    @Query("""
        select booking
        from Booking booking
        left join fetch booking.scheduleBlock block
        left join fetch block.service offering
        left join fetch offering.provider provider
        where booking.user.id = :userId
        order by booking.date desc, booking.startTime desc, booking.id desc
        """)
    List<Booking> findHistoryByUserIdWithDetails(
        @Param("userId") Integer userId
    );

    @Query("""
        select booking
        from Booking booking
        left join fetch booking.user customer
        left join fetch booking.scheduleBlock block
        left join fetch block.service offering
        left join fetch offering.provider provider
        where booking.id = :bookingId
          and booking.user.id = :userId
        """)
    Optional<Booking> findOwnedByIdWithDetails(
        @Param("bookingId") Integer bookingId,
        @Param("userId") Integer userId
    );

    @Query(value = """
        select exists (
            select 1
            from reserva booking
            where booking.disponibilidad_servicio_id = :scheduleBlockId
              and booking.fecha = :date
              and booking.estado = 'activa'
              and booking.hora < :requestedEnd
              and booking.hora_fin > :requestedStart
        )
        """, nativeQuery = true)
    boolean existsActiveOverlap(
        @Param("scheduleBlockId") Integer scheduleBlockId,
        @Param("date") LocalDate date,
        @Param("requestedStart") LocalTime requestedStart,
        @Param("requestedEnd") LocalTime requestedEnd
    );

    long countByScheduleBlock_Id(Integer scheduleBlockId);

    void deleteAllByScheduleBlock_Id(Integer scheduleBlockId);
}
