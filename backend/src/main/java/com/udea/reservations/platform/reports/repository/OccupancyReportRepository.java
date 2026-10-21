package com.udea.reservations.platform.reports.repository;

import com.udea.reservations.platform.bookings.model.Booking;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OccupancyReportRepository
    extends Repository<Booking, Integer> {

    @Query(value = """
        select
            count(*) as "totalBookings",
            count(*) filter (where estado in ('activa', 'completada'))
                as "effectiveBookings",
            count(*) filter (where estado = 'cancelada')
                as "cancelledBookings",
            count(*) filter (where disponibilidad_servicio_id is null)
                as "bookingsWithoutServiceAttribution"
        from reserva
        where fecha between :startDate and :endDate
        """, nativeQuery = true)
    OccupancySummaryProjection findSummary(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query(value = """
        select
            service.id as "serviceId",
            service.nombre as "serviceName",
            count(*) filter (where booking.estado in ('activa', 'completada'))
                as "effectiveBookings",
            count(*) filter (where booking.estado = 'cancelada')
                as "cancelledBookings",
            coalesce(
                sum(
                    case
                        when booking.estado in ('activa', 'completada')
                             and booking.hora_fin is not null
                        then extract(
                            epoch from (booking.hora_fin - booking.hora)
                        ) / 60
                        else 0
                    end
                ),
                0
            )::bigint as "reservedMinutes"
        from reserva booking
        join disponibilidad_servicio availability
            on availability.id = booking.disponibilidad_servicio_id
        join servicio service
            on service.id = availability.servicio_id
        where booking.fecha between :startDate and :endDate
        group by service.id, service.nombre
        order by service.nombre, service.id
        """, nativeQuery = true)
    List<ServiceOccupancyProjection> findServiceMetrics(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query(value = """
        select
            resource_entry.id as "resourceId",
            resource_entry.nombre as "resourceName",
            count(*) as "effectiveBookings"
        from reserva booking
        join horario_disponible available_schedule
            on available_schedule.id = booking.horario_id
        join agenda
            on agenda.id = available_schedule.agenda_id
        join recurso resource_entry
            on resource_entry.id = agenda.recurso_id
        where booking.fecha between :startDate and :endDate
          and booking.estado in ('activa', 'completada')
        group by resource_entry.id, resource_entry.nombre
        order by resource_entry.nombre, resource_entry.id
        """, nativeQuery = true)
    List<ResourceOccupancyProjection> findResourceMetrics(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    interface OccupancySummaryProjection {

        long getTotalBookings();

        long getEffectiveBookings();

        long getCancelledBookings();

        long getBookingsWithoutServiceAttribution();
    }

    interface ServiceOccupancyProjection {

        Integer getServiceId();

        String getServiceName();

        long getEffectiveBookings();

        long getCancelledBookings();

        long getReservedMinutes();
    }

    interface ResourceOccupancyProjection {

        Integer getResourceId();

        String getResourceName();

        long getEffectiveBookings();
    }
}
