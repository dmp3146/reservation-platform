package com.udea.reservations.platform.schedules.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import com.udea.reservations.platform.schedules.dto.ScheduleCreateRequest;
import com.udea.reservations.platform.schedules.dto.ScheduleResponse;
import com.udea.reservations.platform.schedules.exception.ScheduleDateInPastException;
import com.udea.reservations.platform.schedules.exception.ScheduleOverlapException;
import com.udea.reservations.platform.schedules.exception.ScheduleTimeRangeInvalidException;
import com.udea.reservations.platform.schedules.exception.ServiceNotFoundException;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.users.model.User;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private ServiceOffering ownedService;

    @BeforeEach
    void setUp() {
        Provider provider = new Provider();
        provider.setId(7);
        User user = new User();
        user.setId(42);
        provider.setUser(user);
        ownedService = new ServiceOffering();
        ownedService.setId(15);
        ownedService.setProvider(provider);
        ownedService.setActive(true);
        lenient().when(serviceRepository.findByIdForUpdate(15))
            .thenReturn(Optional.of(ownedService));
    }

    @Test
    void createsValidScheduleBlockForOwnedService() {
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );
        when(scheduleBlockRepository.countOverlapping(
            15, request.fecha(), request.horaInicio(), request.horaFin()
        )).thenReturn(0L);
        when(scheduleBlockRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> {
                ScheduleBlock block = invocation.getArgument(0);
                block.setId(31);
                return block;
            });

        ScheduleResponse response = scheduleService.createForProvider(42, request);

        assertThat(response).isEqualTo(new ScheduleResponse(
            31,
            15,
            request.fecha(),
            request.horaInicio(),
            request.horaFin()
        ));
        ArgumentCaptor<ScheduleBlock> captor =
            ArgumentCaptor.forClass(ScheduleBlock.class);
        verify(scheduleBlockRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getService()).isSameAs(ownedService);
    }

    @Test
    void rejectsNewScheduleForInactiveService() {
        ownedService.setActive(false);
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        assertThatThrownBy(() -> scheduleService.createForProvider(42, request))
            .isInstanceOf(ServiceNotFoundException.class);

        verify(scheduleBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void acceptsAdjacentBlocks() {
        LocalDate date = LocalDate.now();
        ScheduleCreateRequest first = request(
            15,
            date,
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );
        ScheduleCreateRequest adjacent = request(
            15,
            date,
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
        );
        when(scheduleBlockRepository.countOverlapping(
            15, date, LocalTime.of(9, 0), LocalTime.of(10, 0)
        )).thenReturn(0L);
        when(scheduleBlockRepository.countOverlapping(
            15, date, LocalTime.of(10, 0), LocalTime.of(11, 0)
        )).thenReturn(0L);
        when(scheduleBlockRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.createForProvider(42, first);
        scheduleService.createForProvider(42, adjacent);

        verify(scheduleBlockRepository, times(2)).saveAndFlush(any());
    }

    @Test
    void rejectsPastDateWithoutPersisting() {
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().minusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        assertThatThrownBy(() -> scheduleService.createForProvider(42, request))
            .isInstanceOf(ScheduleDateInPastException.class);

        verify(scheduleBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsEqualOrReversedTimeRangeWithoutPersisting() {
        for (LocalTime end : List.of(LocalTime.of(9, 0), LocalTime.of(8, 59))) {
            ScheduleCreateRequest request = request(
                15,
                LocalDate.now(),
                LocalTime.of(9, 0),
                end
            );

            assertThatThrownBy(() -> scheduleService.createForProvider(42, request))
                .isInstanceOf(ScheduleTimeRangeInvalidException.class);
        }

        verify(scheduleBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsOverlappingBlockWithoutPersisting() {
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 30),
            LocalTime.of(10, 30)
        );
        when(scheduleBlockRepository.countOverlapping(
            15, request.fecha(), request.horaInicio(), request.horaFin()
        )).thenReturn(1L);

        assertThatThrownBy(() -> scheduleService.createForProvider(42, request))
            .isInstanceOf(ScheduleOverlapException.class);

        verify(scheduleBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesPostgresOverlapTriggerFailureToConflict() {
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );
        when(scheduleBlockRepository.countOverlapping(
            15, request.fecha(), request.horaInicio(), request.horaFin()
        )).thenReturn(0L);
        when(scheduleBlockRepository.saveAndFlush(any())).thenThrow(
            new DataIntegrityViolationException(
                "overlap",
                new SQLException("overlap", "23P01")
            )
        );

        assertThatThrownBy(() -> scheduleService.createForProvider(42, request))
            .isInstanceOf(ScheduleOverlapException.class);
    }

    @Test
    void rejectsServiceOwnedByAnotherProviderWithoutRevealingIt() {
        ScheduleCreateRequest request = request(
            15,
            LocalDate.now().plusDays(1),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0)
        );

        assertThatThrownBy(() -> scheduleService.createForProvider(99, request))
            .isInstanceOf(ServiceNotFoundException.class);

        verify(scheduleBlockRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsBlocksOrderedByDateAndStartTime() {
        ScheduleBlock first = new ScheduleBlock();
        first.setId(1);
        first.setService(ownedService);
        first.setDate(LocalDate.now().plusDays(1));
        first.setStartTime(LocalTime.of(9, 0));
        first.setEndTime(LocalTime.of(10, 0));
        ScheduleBlock second = new ScheduleBlock();
        second.setId(2);
        second.setService(ownedService);
        second.setDate(LocalDate.now().plusDays(1));
        second.setStartTime(LocalTime.of(10, 0));
        second.setEndTime(LocalTime.of(11, 0));
        List<ScheduleBlock> blocks = List.of(first, second);
        when(serviceRepository.existsById(15)).thenReturn(true);
        when(scheduleBlockRepository.findAllByService_IdOrderByDateAscStartTimeAsc(15))
            .thenReturn(blocks);

        List<ScheduleResponse> response = scheduleService.findByServiceId(15);

        assertThat(response).containsExactly(
            new ScheduleResponse(
                1,
                15,
                first.getDate(),
                first.getStartTime(),
                first.getEndTime()
            ),
            new ScheduleResponse(
                2,
                15,
                second.getDate(),
                second.getStartTime(),
                second.getEndTime()
            )
        );
        verify(scheduleBlockRepository)
            .findAllByService_IdOrderByDateAscStartTimeAsc(15);
    }

    @Test
    void rejectsScheduleLookupForUnknownService() {
        when(serviceRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.findByServiceId(99))
            .isInstanceOf(ServiceNotFoundException.class);

        verify(scheduleBlockRepository, never())
            .findAllByService_IdOrderByDateAscStartTimeAsc(99);
    }

    @Test
    void returnsFutureSchedulesOnlyForActiveService() {
        ScheduleBlock future = new ScheduleBlock();
        future.setId(31);
        future.setService(ownedService);
        future.setDate(LocalDate.now().plusDays(1));
        future.setStartTime(LocalTime.of(9, 0));
        future.setEndTime(LocalTime.of(10, 0));
        when(serviceRepository.existsById(15)).thenReturn(true);
        when(scheduleBlockRepository.findFutureByServiceId(
            org.mockito.ArgumentMatchers.eq(15),
            org.mockito.ArgumentMatchers.any(LocalDate.class),
            org.mockito.ArgumentMatchers.any(LocalTime.class)
        )).thenReturn(List.of(future));

        assertThat(scheduleService.findFutureByServiceId(15)).containsExactly(
            new ScheduleResponse(
                31,
                15,
                future.getDate(),
                future.getStartTime(),
                future.getEndTime()
            )
        );
    }

    @Test
    void rejectsFutureScheduleLookupForUnknownService() {
        when(serviceRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.findFutureByServiceId(99))
            .isInstanceOf(ServiceNotFoundException.class);

        verify(scheduleBlockRepository, never()).findFutureByServiceId(
            org.mockito.ArgumentMatchers.eq(99),
            org.mockito.ArgumentMatchers.any(LocalDate.class),
            org.mockito.ArgumentMatchers.any(LocalTime.class)
        );
    }

    private static ScheduleCreateRequest request(
        Integer serviceId,
        LocalDate date,
        LocalTime start,
        LocalTime end
    ) {
        return new ScheduleCreateRequest(serviceId, date, start, end);
    }
}
