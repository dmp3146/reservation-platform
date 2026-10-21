package com.udea.reservations.platform.bookings.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.udea.reservations.platform.bookings.dto.BookingCreateRequest;
import com.udea.reservations.platform.bookings.exception.BookingConflictException;
import com.udea.reservations.platform.bookings.repository.BookingRepository;
import com.udea.reservations.platform.bookings.service.BookingService;
import com.udea.reservations.platform.providers.model.Provider;
import com.udea.reservations.platform.providers.repository.ProviderRepository;
import com.udea.reservations.platform.schedules.model.ScheduleBlock;
import com.udea.reservations.platform.schedules.repository.ScheduleBlockRepository;
import com.udea.reservations.platform.services.model.ServiceOffering;
import com.udea.reservations.platform.services.repository.ServiceRepository;
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
class BookingConcurrencyPostgresIntegrationTest {

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
    private BookingService bookingService;

    private Integer userId;
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
    void createBookingFixture() {
        String suffix = UUID.randomUUID().toString();

        User customer = new User();
        customer.setName("Booking Integration Test");
        customer.setEmail("booking-test-" + suffix + "@example.invalid");
        customer.setPasswordHash("not-used-by-this-test");
        customer.setRole("cliente");
        userId = userRepository.saveAndFlush(customer).getId();

        User providerUser = new User();
        providerUser.setName("Booking Provider Test");
        providerUser.setEmail("booking-provider-" + suffix + "@example.invalid");
        providerUser.setPasswordHash("not-used-by-this-test");
        providerUser.setRole("proveedor");
        providerUser = userRepository.saveAndFlush(providerUser);
        providerUserId = providerUser.getId();

        Provider provider = new Provider();
        provider.setUser(providerUser);
        provider.setBusinessName("Booking Test " + suffix);
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering service = new ServiceOffering();
        service.setProvider(provider);
        service.setName("Booking Test Service");
        service.setDescription("Integration test fixture");
        service.setDurationMinutes(30);
        service.setActive(true);
        serviceId = serviceRepository.saveAndFlush(service).getId();

        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);
        block.setDate(LocalDate.now().plusDays(30));
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(12, 0));
        scheduleBlockId = scheduleBlockRepository.saveAndFlush(block).getId();
    }

    @AfterEach
    void removeBookingFixture() {
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
        if (userId != null) {
            userRepository.deleteById(userId);
        }
        if (providerUserId != null) {
            userRepository.deleteById(providerUserId);
        }
    }

    @Test
    void concurrentRequestsForSameTimePersistOnlyOneBooking() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> createConcurrently(ready, start));
            var second = executor.submit(() -> createConcurrently(ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successfulBookings = (first.get() ? 1 : 0)
                + (second.get() ? 1 : 0);
            assertThat(successfulBookings).isEqualTo(1);
            assertThat(bookingRepository.countByScheduleBlock_Id(scheduleBlockId))
                .isEqualTo(1);
        } finally {
            start.countDown();
        }
    }

    private boolean createConcurrently(
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent booking was not released.");
        }
        try {
            bookingService.create(
                userId,
                new BookingCreateRequest(scheduleBlockId, LocalTime.of(9, 0))
            );
            return true;
        } catch (BookingConflictException _) {
            return false;
        }
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
