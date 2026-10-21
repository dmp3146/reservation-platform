package com.udea.reservations.platform.schedules.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(
    named = "SCHEDULES_POSTGRES_TEST_URL",
    matches = ".+"
)
class ScheduleOverlapPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ScheduleBlockRepository scheduleBlockRepository;

    private Integer userId;
    private Integer providerId;
    private Integer serviceId;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url",
            () -> requiredEnvironmentValue("SCHEDULES_POSTGRES_TEST_URL")
        );
        registry.add(
            "spring.datasource.username",
            () -> requiredEnvironmentValue("SCHEDULES_POSTGRES_TEST_USERNAME")
        );
        registry.add(
            "spring.datasource.password",
            () -> requiredEnvironmentValue("SCHEDULES_POSTGRES_TEST_PASSWORD")
        );
    }

    @BeforeEach
    void createServiceFixture() {
        String suffix = UUID.randomUUID().toString();
        User user = new User();
        user.setName("Schedule Integration Test");
        user.setEmail("schedule-test-" + suffix + "@example.invalid");
        user.setPasswordHash("not-used-by-this-test");
        user.setRole("proveedor");
        userId = userRepository.saveAndFlush(user).getId();

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setBusinessName("Schedule Test " + suffix);
        providerId = providerRepository.saveAndFlush(provider).getId();

        ServiceOffering service = new ServiceOffering();
        service.setProvider(provider);
        service.setName("Schedule Test Service");
        service.setDescription("Integration test fixture");
        service.setDurationMinutes(30);
        serviceId = serviceRepository.saveAndFlush(service).getId();
    }

    @AfterEach
    void removeServiceFixture() {
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
    }

    @Test
    void postgresTriggerRejectsConcurrentOverlappingInserts() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> insertConcurrently(ready, start));
            var second = executor.submit(() -> insertConcurrently(ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successfulInserts = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertThat(successfulInserts).isEqualTo(1);
            assertThat(scheduleBlockRepository.countByService_Id(serviceId))
                .isEqualTo(1);
        } finally {
            start.countDown();
        }
    }

    @Test
    void postgresCheckConstraintRejectsInvalidTimeRange() {
        ServiceOffering service = serviceRepository.findById(serviceId)
            .orElseThrow();
        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);
        block.setDate(LocalDate.now().plusDays(10));
        block.setStartTime(LocalTime.of(10, 0));
        block.setEndTime(LocalTime.of(9, 0));

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> scheduleBlockRepository.saveAndFlush(block)
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void postgresAllowsAdjacentBlocks() {
        ServiceOffering service = serviceRepository.findById(serviceId)
            .orElseThrow();
        LocalDate date = LocalDate.now().plusDays(10);
        ScheduleBlock first = new ScheduleBlock();
        first.setService(service);
        first.setDate(date);
        first.setStartTime(LocalTime.of(9, 0));
        first.setEndTime(LocalTime.of(10, 0));
        scheduleBlockRepository.saveAndFlush(first);

        ScheduleBlock adjacent = new ScheduleBlock();
        adjacent.setService(service);
        adjacent.setDate(date);
        adjacent.setStartTime(LocalTime.of(10, 0));
        adjacent.setEndTime(LocalTime.of(11, 0));
        scheduleBlockRepository.saveAndFlush(adjacent);

        assertThat(scheduleBlockRepository.countByService_Id(serviceId))
            .isEqualTo(2);
    }

    private boolean insertConcurrently(
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent insert was not released.");
        }
        ServiceOffering service = serviceRepository.findById(serviceId)
            .orElseThrow();
        ScheduleBlock block = new ScheduleBlock();
        block.setService(service);
        block.setDate(LocalDate.now().plusDays(10));
        block.setStartTime(LocalTime.of(9, 0));
        block.setEndTime(LocalTime.of(10, 0));
        try {
            scheduleBlockRepository.saveAndFlush(block);
            return true;
        } catch (DataIntegrityViolationException exception) {
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
