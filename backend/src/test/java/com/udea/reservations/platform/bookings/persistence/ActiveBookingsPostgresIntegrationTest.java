package com.udea.reservations.platform.bookings.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.udea.reservations.platform.bookings.exception.BookingNotFoundException;
import com.udea.reservations.platform.bookings.model.Booking;
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
class ActiveBookingsPostgresIntegrationTest {

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
    private Integer otherCustomerId;
    private Integer providerUserId;
    private Integer providerId;
    private Integer serviceId;
    private Integer scheduleBlockId;
    private Integer ownedBookingId;
    private Integer otherCustomerBookingId;

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
    void createBookingFixtures() {
        String suffix = UUID.randomUUID().toString();
        customerId = createUser("active-bookings-" + suffix, "cliente");
        otherCustomerId = createUser("other-bookings-" + suffix, "cliente");
        providerUserId = createUser("active-provider-" + suffix, "proveedor");

        User providerUser = userRepository.findById(providerUserId).orElseThrow();
        Provider provider = new Provider();
        provider.setUser(providerUser);
        provider.setBusinessName("Active Booking Provider " + suffix);
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering service = new ServiceOffering();
        service.setProvider(provider);
        service.setName("Active Booking Service");
        service.setDescription("Integration test fixture");
        service.setDurationMinutes(30);
        service.setActive(true);
        serviceId = serviceRepository.saveAndFlush(service).getId();

        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);
        block.setDate(LocalDate.now().plusDays(30));
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(14, 0));
        scheduleBlockId = scheduleBlockRepository.saveAndFlush(block).getId();

        ownedBookingId = createBooking(
            customerId,
            LocalTime.of(11, 0),
            "activa"
        );
        createBooking(customerId, LocalTime.of(9, 0), "activa");
        createBooking(customerId, LocalTime.of(12, 0), "cancelada");
        createBooking(customerId, LocalTime.of(12, 30), "completada");
        otherCustomerBookingId = createBooking(
            otherCustomerId,
            LocalTime.of(13, 0),
            "activa"
        );
    }

    @AfterEach
    void removeBookingFixtures() {
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
        if (otherCustomerId != null) {
            userRepository.deleteById(otherCustomerId);
        }
        if (providerUserId != null) {
            userRepository.deleteById(providerUserId);
        }
    }

    @Test
    void queryReturnsOnlyOwnActiveBookingsOrderedAndWithServiceDetails() {
        var bookings = bookingService.findActiveForCustomer(customerId);

        assertThat(bookings).hasSize(2);
        assertThat(bookings.get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(bookings.get(1).startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(bookings)
            .allSatisfy(booking -> {
                assertThat(booking.serviceName())
                    .isEqualTo("Active Booking Service");
                assertThat(booking.providerName())
                    .startsWith("Active Booking Provider ");
                assertThat(booking.status()).isEqualTo("activa");
                assertThat(booking.endTime())
                    .isEqualTo(booking.startTime().plusMinutes(30));
            });
    }

    @Test
    void queryReturnsEmptyListForCustomerWithoutActiveBookings() {
        Integer customerWithoutBookings = createUser(
            "empty-bookings-" + UUID.randomUUID(),
            "cliente"
        );

        assertThat(bookingService.findActiveForCustomer(customerWithoutBookings))
            .isEmpty();

        userRepository.deleteById(customerWithoutBookings);
    }

    @Test
    void historyReturnsAllOwnStatesInReverseChronologicalOrder() {
        var bookings = bookingService.findHistoryForCustomer(customerId);

        assertThat(bookings).hasSize(4);
        assertThat(bookings)
            .extracting(booking -> booking.startTime())
            .containsExactly(
                LocalTime.of(12, 30),
                LocalTime.of(12, 0),
                LocalTime.of(11, 0),
                LocalTime.of(9, 0)
            );
        assertThat(bookings)
            .extracting(booking -> booking.status())
            .containsExactly("completada", "cancelada", "activa", "activa");
        assertThat(bookings)
            .allSatisfy(booking -> {
                assertThat(booking.serviceName())
                    .isEqualTo("Active Booking Service");
                assertThat(booking.providerName())
                    .startsWith("Active Booking Provider ");
            });
    }

    @Test
    void detailLoadsServiceProviderAndCustomerOnlyForOwner() {
        var detail = bookingService.findOwnedBooking(customerId, ownedBookingId);

        assertThat(detail.bookingId()).isEqualTo(ownedBookingId);
        assertThat(detail.serviceName()).isEqualTo("Active Booking Service");
        assertThat(detail.providerName())
            .startsWith("Active Booking Provider ");
        assertThat(detail.customerName()).isEqualTo("Active booking test");

        assertThatThrownBy(
            () -> bookingService.findOwnedBooking(customerId, otherCustomerBookingId)
        ).isInstanceOf(BookingNotFoundException.class);
    }

    private Integer createUser(String emailPrefix, String role) {
        User user = new User();
        user.setName("Active booking test");
        user.setEmail(emailPrefix + "@example.invalid");
        user.setPasswordHash("not-used-by-this-test");
        user.setRole(role);
        return userRepository.saveAndFlush(user).getId();
    }

    private Integer createBooking(
        Integer bookingCustomerId,
        LocalTime startTime,
        String status
    ) {
        User customer = userRepository.findById(bookingCustomerId).orElseThrow();
        ScheduleBlock block = scheduleBlockRepository
            .findById(scheduleBlockId)
            .orElseThrow();
        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setScheduleBlock(block);
        booking.setDate(block.getDate());
        booking.setStartTime(startTime);
        booking.setEndTime(startTime.plusMinutes(30));
        booking.setStatus(status);
        return bookingRepository.saveAndFlush(booking).getId();
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
