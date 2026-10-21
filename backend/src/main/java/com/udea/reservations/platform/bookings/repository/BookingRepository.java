package com.udea.reservations.platform.bookings.repository;

import com.udea.reservations.platform.bookings.model.Booking;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

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
