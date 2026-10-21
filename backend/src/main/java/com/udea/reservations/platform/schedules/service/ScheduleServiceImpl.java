package com.udea.reservations.platform.schedules.service;

import com.udea.reservations.platform.schedules.dto.ScheduleCreateRequest;
import com.udea.reservations.platform.schedules.dto.ScheduleResponse;
import com.udea.reservations.platform.schedules.exception.ScheduleDateInPastException;
import com.udea.reservations.platform.schedules.exception.ScheduleOverlapException;
import com.udea.reservations.platform.schedules.exception.ScheduleTimeRangeInvalidException;
import com.udea.reservations.platform.schedules.exception.ServiceNotFoundException;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private static final String OVERLAP_SQL_STATE = "23P01";

    private final ServiceRepository serviceRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;

    @Override
    @Transactional
    public ScheduleResponse createForProvider(
        Integer authenticatedUserId,
        ScheduleCreateRequest request
    ) {
        if (request.fecha().isBefore(LocalDate.now(ZoneId.systemDefault()))) {
            throw new ScheduleDateInPastException();
        }
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new ScheduleTimeRangeInvalidException();
        }

        ServiceOffering service = serviceRepository
            .findByIdForUpdate(request.serviceId())
            .filter(candidate -> Objects.equals(
                candidate.getProvider().getUser().getId(),
                authenticatedUserId
            ))
            .orElseThrow(ServiceNotFoundException::new);

        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new ServiceNotFoundException();
        }

        if (scheduleBlockRepository.countOverlapping(
            service.getId(),
            request.fecha(),
            request.horaInicio(),
            request.horaFin()
        ) > 0) {
            throw new ScheduleOverlapException();
        }

        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);
        block.setDate(request.fecha());
        block.setStartTime(request.horaInicio());
        block.setEndTime(request.horaFin());

        try {
            ScheduleBlock saved = scheduleBlockRepository.saveAndFlush(block);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (containsOverlapSqlState(exception)) {
                throw new ScheduleOverlapException();
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findByServiceId(Integer serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ServiceNotFoundException();
        }
        return scheduleBlockRepository
            .findAllByService_IdOrderByDateAscStartTimeAsc(serviceId)
            .stream()
            .map(ScheduleServiceImpl::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findFutureByServiceId(Integer serviceId) {
        if (!serviceRepository.existsById(serviceId)) {
            throw new ServiceNotFoundException();
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        return scheduleBlockRepository.findFutureByServiceId(
                serviceId,
                now.toLocalDate(),
                now.toLocalTime()
            )
            .stream()
            .map(ScheduleServiceImpl::toResponse)
            .toList();
    }

    private static ScheduleResponse toResponse(ScheduleBlock block) {
        return new ScheduleResponse(
            block.getId(),
            block.getService().getId(),
            block.getDate(),
            block.getStartTime(),
            block.getEndTime()
        );
    }

    private static boolean containsOverlapSqlState(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                && OVERLAP_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
