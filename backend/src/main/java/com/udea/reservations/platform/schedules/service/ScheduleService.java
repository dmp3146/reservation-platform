package com.udea.reservations.platform.schedules.service;

import com.udea.reservations.platform.schedules.dto.ScheduleCreateRequest;
import com.udea.reservations.platform.schedules.dto.ScheduleResponse;
import java.util.List;

public interface ScheduleService {

    ScheduleResponse createForProvider(
        Integer authenticatedUserId,
        ScheduleCreateRequest request
    );

    List<ScheduleResponse> findByServiceId(Integer serviceId);

    List<ScheduleResponse> findFutureByServiceId(Integer serviceId);
}
