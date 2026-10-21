package com.udea.reservations.platform.reports.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.reports.dto.OccupancyReportResponse;
import com.udea.reservations.platform.reports.service.OccupancyReportService;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(
    named = "BOOKINGS_POSTGRES_TEST_URL",
    matches = ".+"
)
class OccupancyReportPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ScheduleBlockRepository scheduleBlockRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OccupancyReportService reportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Integer> bookingIds = new ArrayList<>();
    private final List<Integer> scheduleBlockIds = new ArrayList<>();
    private Integer customerId;
    private Integer providerUserId;
    private Integer providerId;
    private Integer serviceId;
    private Integer resourceId;
    private Integer agendaId;
    private Integer availableScheduleId;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> requiredEnvironmentValue("BOOKINGS_POSTGRES_TEST_URL")
        );
        registry.add(
            "spring.datasource.username",
            () -> requiredEnvironmentValue("BOOKINGS_POSTGRES_TEST_USERNAME")
        );
        registry.add(
            "spring.datasource.password",
            () -> requiredEnvironmentValue("BOOKINGS_POSTGRES_TEST_PASSWORD")
        );
    }

    @BeforeEach
    void createReportFixtures() {
        String suffix = UUID.randomUUID().toString();
        customerId = createUser("occupancy-client-" + suffix, "cliente");
        providerUserId = createUser("occupancy-provider-" + suffix, "proveedor");

        Provider provider = new Provider();
        provider.setUser(userRepository.findById(providerUserId).orElseThrow());
        provider.setBusinessName("Occupancy report provider " + suffix);
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering service = new ServiceOffering();
        service.setProvider(provider);
        service.setName("Occupancy report service");
        service.setDescription("Integration fixture");
        service.setDurationMinutes(30);
        service.setActive(true);
        serviceId = serviceRepository.saveAndFlush(service).getId();

        resourceId = jdbcTemplate.queryForObject(
            """
                insert into recurso (proveedor_id, nombre, tipo)
                values (?, ?, ?)
                returning id
                """,
            Integer.class,
            providerId,
            "Occupancy report room " + suffix,
            "consultorio"
        );
        agendaId = jdbcTemplate.queryForObject(
            """
                insert into agenda (proveedor_id, recurso_id, nombre)
                values (?, ?, ?)
                returning id
                """,
            Integer.class,
            providerId,
            resourceId,
            "Occupancy report agenda " + suffix
        );
        availableScheduleId = jdbcTemplate.queryForObject(
            """
                insert into horario_disponible (
                    agenda_id, dia_semana, hora_inicio, hora_fin,
                    duracion_slot_min
                )
                values (?, 1, '09:00', '14:00', 30)
                returning id
                """,
            Integer.class,
            agendaId
        );
    }

    @AfterEach
    void removeReportFixtures() {
        bookingIds.forEach(bookingRepository::deleteById);
        if (availableScheduleId != null) {
            jdbcTemplate.update(
                "delete from horario_disponible where id = ?",
                availableScheduleId
            );
        }
        if (agendaId != null) {
            jdbcTemplate.update("delete from agenda where id = ?", agendaId);
        }
        if (resourceId != null) {
            jdbcTemplate.update("delete from recurso where id = ?", resourceId);
        }
        scheduleBlockIds.forEach(scheduleBlockRepository::deleteById);
        if (serviceId != null) {
            serviceRepository.deleteById(serviceId);
        }
        if (providerId != null) {
            providerRepository.deleteById(providerId);
        }
        if (customerId != null) {
            userRepository.deleteById(customerId);
        }
        if (providerUserId != null) {
            userRepository.deleteById(providerUserId);
        }
    }

    @Test
    void reportFiltersInclusiveDateRangeAndAggregatesPersistedStatuses() {
        LocalDate startDate = LocalDate.of(2040, 10, 10);
        LocalDate endDate = LocalDate.of(2040, 10, 12);
        createBooking(startDate.minusDays(1), LocalTime.of(9, 0), "activa");
        createBooking(startDate, LocalTime.of(9, 0), "activa");
        createBooking(endDate, LocalTime.of(10, 0), "completada");
        createBooking(endDate, LocalTime.of(11, 0), "cancelada");
        createBooking(endDate.plusDays(1), LocalTime.of(9, 0), "activa");
        createLegacyBooking(startDate, LocalTime.of(12, 0), "activa");
        createLegacyBooking(endDate, LocalTime.of(13, 0), "cancelada");

        OccupancyReportResponse report =
            reportService.generate(startDate, endDate);

        assertThat(report.totalBookings()).isEqualTo(5);
        assertThat(report.effectiveBookings()).isEqualTo(3);
        assertThat(report.cancelledBookings()).isEqualTo(2);
        assertThat(report.bookingsWithoutServiceAttribution()).isEqualTo(2);
        assertThat(report.services()).hasSize(1);
        assertThat(report.services().get(0).serviceId()).isEqualTo(serviceId);
        assertThat(report.services().get(0).effectiveBookings()).isEqualTo(2);
        assertThat(report.services().get(0).cancelledBookings()).isEqualTo(1);
        assertThat(report.services().get(0).reservedMinutes()).isEqualTo(60);
        assertThat(report.resources()).hasSize(1);
        assertThat(report.resources().get(0).resourceId()).isEqualTo(resourceId);
        assertThat(report.resources().get(0).effectiveBookings()).isEqualTo(1);
    }

    @Test
    void reportReturnsZeroMetricsForPeriodWithoutBookings() {
        OccupancyReportResponse report = reportService.generate(
            LocalDate.of(2041, 1, 1),
            LocalDate.of(2041, 1, 2)
        );

        assertThat(report.totalBookings()).isZero();
        assertThat(report.effectiveBookings()).isZero();
        assertThat(report.cancelledBookings()).isZero();
        assertThat(report.hasBookings()).isFalse();
        assertThat(report.services()).isEmpty();
    }

    private void createBooking(
        LocalDate date,
        LocalTime startTime,
        String status
    ) {
        ScheduleBlock block = new ScheduleBlock();
        block.setService(serviceRepository.findById(serviceId).orElseThrow());
        block.setDate(date);
        block.setStartTime(startTime);
        block.setEndTime(startTime.plusHours(1));
        Integer blockId = scheduleBlockRepository.saveAndFlush(block).getId();
        scheduleBlockIds.add(blockId);

        Booking booking = new Booking();
        booking.setUser(userRepository.findById(customerId).orElseThrow());
        booking.setScheduleBlock(
            scheduleBlockRepository.findById(blockId).orElseThrow()
        );
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(30));
        booking.setStatus(status);
        bookingIds.add(bookingRepository.saveAndFlush(booking).getId());
    }

    private void createLegacyBooking(
        LocalDate date,
        LocalTime startTime,
        String status
    ) {
        Integer id = jdbcTemplate.queryForObject(
            """
                insert into reserva (
                    usuario_id, horario_id, fecha, hora, estado
                )
                values (?, ?, ?, ?, ?)
                returning id
                """,
            Integer.class,
            customerId,
            availableScheduleId,
            date,
            startTime,
            status
        );
        bookingIds.add(id);
    }

    private Integer createUser(String emailPrefix, String role) {
        User user = new User();
        user.setName("Occupancy report integration test");
        user.setEmail(emailPrefix + "@example.invalid");
        user.setPasswordHash("not-used-by-this-test");
        user.setRole(role);
        return userRepository.saveAndFlush(user).getId();
    }

    private static String requiredEnvironmentValue(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                name + " must be set to run PostgreSQL integration tests."
            );
        }
        return value;
    }
}
