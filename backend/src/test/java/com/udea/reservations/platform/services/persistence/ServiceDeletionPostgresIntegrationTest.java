package com.udea.reservations.platform.services.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.exception.BookingConflictException;
import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.bookings.service.BookingService;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.exception.ServiceHasActiveBookingsException;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
import com.udea.reservations.platform.services.service.ServiceOfferingService;
import com.udea.reservations.platform.users.model.User;
import com.udea.reservations.platform.users.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(
    named = "BOOKINGS_POSTGRES_TEST_URL",
    matches = ".+"
)
class ServiceDeletionPostgresIntegrationTest {

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
    private ServiceOfferingService serviceOfferingService;

    @Autowired
    private BookingService bookingService;

    private Integer customerId;
    private Integer providerUserId;
    private Integer providerId;
    private Integer serviceId;
    private Integer scheduleBlockId;

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
    void createServiceFixture() {
        String suffix = UUID.randomUUID().toString();
        customerId = createUser("service-delete-client-" + suffix, "cliente");
        providerUserId = createUser("service-delete-provider-" + suffix, "proveedor");

        Provider provider = new Provider();
        provider.setUser(userRepository.findById(providerUserId).orElseThrow());
        provider.setBusinessName("Service deletion provider " + suffix);
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering offering = new ServiceOffering();
        offering.setProvider(provider);
        offering.setName("Service deletion test");
        offering.setDescription("Integration test fixture");
        offering.setDurationMinutes(30);
        offering.setActive(true);
        serviceId = serviceRepository.saveAndFlush(offering).getId();

        ScheduleBlock block = new ScheduleBlock();
        block.setService(serviceRepository.findById(serviceId).orElseThrow());
        block.setDate(LocalDate.now().plusDays(30));
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(12, 0));
        scheduleBlockId = scheduleBlockRepository.saveAndFlush(block).getId();
    }

    @AfterEach
    void removeServiceFixture() {
        if (scheduleBlockId != null) {
            bookingRepository.deleteAllByScheduleBlock_Id(scheduleBlockId);
        }
        if (serviceId != null) {
            scheduleBlockRepository.deleteAllByService_Id(serviceId);
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
    void retiresServiceAndPreservesHistoricalBookingsAndAvailability() {
        Booking cancelled = createBooking(LocalTime.of(9, 30), "cancelada");
        Booking completed = createBooking(LocalTime.of(10, 0), "completada");

        serviceOfferingService.deleteForProvider(providerUserId, serviceId);

        ServiceOffering retired = serviceRepository.findById(serviceId)
            .orElseThrow();
        assertThat(retired.getActive()).isFalse();
        assertThat(scheduleBlockRepository.countByService_Id(serviceId)).isEqualTo(1);
        assertThat(bookingRepository.findById(cancelled.getId())
            .orElseThrow().getStatus()).isEqualTo("cancelada");
        assertThat(bookingRepository.findById(completed.getId())
            .orElseThrow().getStatus()).isEqualTo("completada");
    }

    @Test
    void rejectsRetirementWhenServiceHasActiveBookingWithoutChangingRecords() {
        Booking active = createBooking(LocalTime.of(9, 30), "activa");

        assertThatThrownBy(() -> serviceOfferingService.deleteForProvider(
            providerUserId,
            serviceId
        )).isInstanceOf(ServiceHasActiveBookingsException.class);

        assertThat(serviceRepository.findById(serviceId).orElseThrow().getActive())
            .isTrue();
        assertThat(bookingRepository.findById(active.getId()).orElseThrow().getStatus())
            .isEqualTo("activa");
    }

    @Test
    void bookingAndRetirementCannotBothSucceedConcurrently() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var booking = executor.submit(() -> bookConcurrently(ready, start));
            var retirement = executor.submit(
                () -> retireConcurrently(ready, start)
            );
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            boolean bookingSucceeded = booking.get();
            boolean retirementSucceeded = retirement.get();
            assertThat(bookingSucceeded).isNotEqualTo(retirementSucceeded);

            ServiceOffering storedService = serviceRepository.findById(serviceId)
                .orElseThrow();
            boolean hasActiveBooking =
                bookingRepository.existsByScheduleBlock_Service_IdAndStatus(
                    serviceId,
                    "activa"
                );
            assertThat(hasActiveBooking).isEqualTo(storedService.getActive());
        } finally {
            start.countDown();
        }
    }

    private boolean bookConcurrently(
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent operation was not released.");
        }
        try {
            bookingService.create(
                customerId,
                new BookingCreateRequest(scheduleBlockId, LocalTime.of(9, 30))
            );
            return true;
        } catch (BookingConflictException _) {
            return false;
        }
    }

    private boolean retireConcurrently(
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent operation was not released.");
        }
        try {
            serviceOfferingService.deleteForProvider(providerUserId, serviceId);
            return true;
        } catch (ServiceHasActiveBookingsException _) {
            return false;
        }
    }

    private Booking createBooking(LocalTime startTime, String status) {
        Booking booking = new Booking();
        booking.setUser(userRepository.findById(customerId).orElseThrow());
        booking.setScheduleBlock(
            scheduleBlockRepository.findById(scheduleBlockId).orElseThrow()
        );
        booking.setDate(LocalDate.now().plusDays(30));
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(30));
        booking.setStatus(status);
        return bookingRepository.saveAndFlush(booking);
    }

    private Integer createUser(String emailPrefix, String role) {
        User user = new User();
        user.setName("Service deletion test");
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
