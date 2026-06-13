package com.bankrestapi.integration;

import com.bankrestapi.dto.BankDtos.TransferRequest;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import com.bankrestapi.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ConcurrentTransferIntegrationTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> false);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired UserRepository users;
    @Autowired AccountRepository accounts;
    @Autowired BankService bankService;
    @Autowired PasswordEncoder passwordEncoder;

    Long sourceId;
    Long targetId;

    @BeforeEach
    void setUp() {
        User owner = users.save(User.builder().username("concurrent-owner").password("hash")
                .email("owner@test.local").fullName("Owner").phone("190001").role(Role.CUSTOMER)
                .enabled(true).kyc(true).pin(passwordEncoder.encode("1234")).build());
        User receiver = users.save(User.builder().username("concurrent-receiver").password("hash")
                .email("receiver@test.local").fullName("Receiver").phone("190002").role(Role.CUSTOMER)
                .enabled(true).kyc(true).build());
        sourceId = accounts.save(Account.builder().accountNumber("RB-CONCURRENT-SOURCE").owner(owner)
                .balance(new BigDecimal("100.00")).currency("VND").active(true).build()).getId();
        targetId = accounts.save(Account.builder().accountNumber("RB-CONCURRENT-TARGET").owner(receiver)
                .balance(BigDecimal.ZERO).currency("VND").active(true).build()).getId();
    }

    @Test
    void concurrentTransfersNeverProduceNegativeBalance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String key = "concurrency-" + i;
            OtpService.Challenge otp = bankService.requestTransferOtp("concurrent-owner");
            tasks.add(() -> {
                try {
                    bankService.transfer(new TransferRequest(sourceId, targetId, new BigDecimal("20.00"),
                            TransactionType.INTERNAL, "Concurrent test", "1234", otp.id(), otp.otp(), key, null, null),
                            "concurrent-owner");
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            });
        }
        long successes = executor.invokeAll(tasks).stream().filter(f -> {
            try { return f.get(); } catch (Exception ex) { return false; }
        }).count();
        executor.shutdown();

        assertEquals(5, successes);
        assertEquals(0, accounts.findById(sourceId).orElseThrow().getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, accounts.findById(targetId).orElseThrow().getBalance().compareTo(new BigDecimal("100.00")));
    }
}
