package com.bankrestapi.config;

import com.bankrestapi.model.*;
import com.bankrestapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedDataConfig {
    @Bean
    ApplicationRunner seedUsers(UserRepository users, PasswordEncoder encoder,
                                @Value("${app.seed.admin-password:Admin@123456}") String adminPassword,
                                @Value("${app.seed.staff-password:Staff@123456}") String staffPassword) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                users.save(User.builder().username("admin").password(encoder.encode(adminPassword))
                        .email("admin@rikkeibank.local").fullName("System Administrator").phone("0000000001")
                        .role(Role.ADMIN).enabled(true).kyc(true).build());
            }
            if (users.findByUsername("staff").isEmpty()) {
                users.save(User.builder().username("staff").password(encoder.encode(staffPassword))
                        .email("staff@rikkeibank.local").fullName("Bank Staff").phone("0000000002")
                        .role(Role.STAFF).enabled(true).kyc(true).build());
            }
        };
    }
}
