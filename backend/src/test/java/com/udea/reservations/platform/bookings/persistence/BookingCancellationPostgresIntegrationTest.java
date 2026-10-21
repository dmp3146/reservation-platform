package com.udea.reservations.platform.bookings.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.udea.reservations.platform.bookings.model.Booking;
import com.udea.reservations.platform.bookings.exception.BookingAlreadyCancelledException;
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
class BookingCancellationPostgresIntegrationTest {

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

    private Integer customerId;
    private Integer providerUserId;
    private Integer providerId;
    private Integer serviceId;
    private Integer scheduleBlockId;
    private Integer bookingId;

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
        customerId = createUser("cancel-customer-" + suffix, "cliente");
        providerUserId = createUser("cancel-provider-" + suffix, "proveedor");

        User providerUser = userRepository.findById(providerUserId).orElseThrow();
        Provider provider = new Provider();
        provider.setUser(providerUser);
        provider.setBusinessName("Cancellation Test Provider");
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering service = new ServiceOffering();
        service.setProvider(provider);
        service.setName("Cancellation Test Service");
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

        User customer = userRepository.findById(customerId).orElseThrow();
        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setScheduleBlock(block);
        booking.setDate(block.getDate());
        booking.setStartTime(LocalTime.of(9, 0));
        booking.setEndTime(LocalTime.of(9, 30));
        booking.setStatus("activa");
        bookingId = bookingRepository.saveAndFlush(booking).getId();
    }

    @AfterEach
    void removeBookingFixture() {
        if (bookingId != null) {
            bookingRepository.deleteById(bookingId);
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
    void cancellationPersistsHistoryAndMakesTheTimeAvailableAgain() {
        LocalTime endTime = LocalTime.of(9, 30);
        assertThat(bookingRepository.existsActiveOverlap(
            scheduleBlockId,
            LocalDate.now().plusDays(30),
            LocalTime.of(9, 0),
            endTime
        )).isTrue();

        var response = bookingService.cancel(customerId, bookingId);

        assertThat(response.status()).isEqualTo("cancelada");
        Booking persistedBooking = bookingRepository.findById(bookingId)
            .orElseThrow();
        assertThat(persistedBooking.getStatus()).isEqualTo("cancelada");
        assertThat(bookingRepository.existsActiveOverlap(
            scheduleBlockId,
            LocalDate.now().plusDays(30),
            LocalTime.of(9, 0),
            endTime
        )).isFalse();
        assertThat(bookingRepository.countByScheduleBlock_Id(scheduleBlockId))
            .isEqualTo(1);
    }

    @Test
    void concurrentCancellationAllowsOnlyOneStateTransition() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> cancelConcurrently(ready, start));
            var second = executor.submit(() -> cancelConcurrently(ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successfulCancellations = (first.get() ? 1 : 0)
                + (second.get() ? 1 : 0);
            assertThat(successfulCancellations).isEqualTo(1);
            Booking persistedBooking = bookingRepository.findById(bookingId)
                .orElseThrow();
            assertThat(persistedBooking.getStatus()).isEqualTo("cancelada");
            assertThat(bookingRepository.countByScheduleBlock_Id(scheduleBlockId))
                .isEqualTo(1);
        } finally {
            start.countDown();
        }
    }

    private boolean cancelConcurrently(
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent cancellation was not released.");
        }
        try {
            bookingService.cancel(customerId, bookingId);
            return true;
        } catch (BookingAlreadyCancelledException _) {
            return false;
        }
    }

    private Integer createUser(String emailPrefix, String role) {
        User user = new User();
        user.setName("Booking cancellation test");
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
