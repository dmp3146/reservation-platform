package com.udea.reservations.platform.schedules.repository;

import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleBlockRepository
    extends JpaRepository<ScheduleBlock, Integer> {

    @Query("""
        select count(block)
        from ScheduleBlock block
        where block.service.id = :serviceId
          and block.date = :date
          and block.startTime < :requestedEnd
          and block.endTime > :requestedStart
        """)
    long countOverlapping(
        @Param("serviceId") Integer serviceId,
        @Param("date") LocalDate date,
        @Param("requestedStart") LocalTime requestedStart,
        @Param("requestedEnd") LocalTime requestedEnd
    );

    List<ScheduleBlock> findAllByService_IdOrderByDateAscStartTimeAsc(
        Integer serviceId
    );

    long countByService_Id(Integer serviceId);

    void deleteAllByService_Id(Integer serviceId);
}
