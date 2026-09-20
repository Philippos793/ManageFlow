package com.philippos.employeemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public ZoneId businessZoneId(
            @Value("${app.business-time-zone:Europe/Athens}") String zoneId) {
        return ZoneId.of(zoneId);
    }

    @Bean
    public Clock businessClock(ZoneId businessZoneId) {
        return Clock.system(businessZoneId);
    }
}
