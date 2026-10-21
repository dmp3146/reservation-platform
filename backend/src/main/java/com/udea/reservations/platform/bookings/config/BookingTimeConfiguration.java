package com.udea.reservations.platform.bookings.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingTimeConfiguration {

    @Bean
    public Clock bookingClock() {
        return Clock.systemDefaultZone();
    }
}
