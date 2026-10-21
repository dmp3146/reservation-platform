package com.udea.reservations.platform.schedules.repository;

import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleBlockRepository
    extends JpaRepository<ScheduleBlock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select block from ScheduleBlock block where block.id = :id")
    Optional<ScheduleBlock> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
        select block.service.id
        from ScheduleBlock block
        where block.id = :id
        """)
    Optional<Integer> findServiceIdById(@Param("id") Integer id);

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

    @Query("""
        select distinct block.service.id
        from ScheduleBlock block
        where block.service.active = true
          and (
              block.date > :today
              or (block.date = :today and block.startTime > :currentTime)
          )
        """)
    List<Integer> findServiceIdsWithFutureSchedules(
        @Param("today") LocalDate today,
        @Param("currentTime") LocalTime currentTime
    );

    @Query("""
        select distinct block.service.id
        from ScheduleBlock block
        where block.service.provider.id = :providerId
          and block.service.active = true
          and (
              block.date > :today
              or (block.date = :today and block.startTime > :currentTime)
          )
        """)
    List<Integer> findServiceIdsWithFutureSchedulesByProviderId(
        @Param("providerId") Integer providerId,
        @Param("today") LocalDate today,
        @Param("currentTime") LocalTime currentTime
    );

    @Query("""
        select block
        from ScheduleBlock block
        where block.service.id = :serviceId
          and block.service.active = true
          and (
              block.date > :today
              or (block.date = :today and block.startTime > :currentTime)
          )
        order by block.date asc, block.startTime asc
        """)
    List<ScheduleBlock> findFutureByServiceId(
        @Param("serviceId") Integer serviceId,
        @Param("today") LocalDate today,
        @Param("currentTime") LocalTime currentTime
    );

    long countByService_Id(Integer serviceId);

    void deleteAllByService_Id(Integer serviceId);
}
