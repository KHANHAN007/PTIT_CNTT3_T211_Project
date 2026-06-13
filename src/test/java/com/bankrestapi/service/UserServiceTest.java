package com.bankrestapi.service;

import com.bankrestapi.dto.UserDtos.*;
import com.bankrestapi.exception.BusinessException;
import com.bankrestapi.model.*;
import com.bankrestapi.repository.*;
import com.bankrestapi.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock AccountRepository accounts;
    @Mock PasswordEncoder encoder;
    @InjectMocks UserServiceImpl service;

    private CreateRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        request = new CreateRequest("customer", "Password123", "c@bank.test", "Customer", "0901", Role.CUSTOMER);
        user = User.builder().id(1L).username("customer").password("hash").email("c@bank.test")
                .fullName("Customer").phone("0901").role(Role.CUSTOMER).enabled(true).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void registerCreatesCustomerAndAccount() {
        when(encoder.encode("Password123")).thenReturn("hash");
        when(users.save(any())).thenReturn(user);
        UserResponse result = service.registerCustomer(request);
        assertEquals(Role.CUSTOMER, result.role());
        verify(accounts).save(any(Account.class));
    }

    @Test
    void registerRejectsDuplicateIdentity() {
        when(users.existsByUsernameOrEmailOrPhone(anyString(), anyString(), anyString())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.registerCustomer(request));
    }

    @Test
    void listUsesProjection() {
        Pageable pageable = PageRequest.of(0, 10);
        when(users.projectUsers("cus", pageable)).thenReturn(Page.empty(pageable));
        assertTrue(service.list("cus", pageable).isEmpty());
    }

    @Test
    void findByIdReturnsMappedUser() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        assertEquals("customer", service.findById(1L).username());
    }

    @Test
    void findByUsernameReturnsMappedUser() {
        when(users.findByUsername("customer")).thenReturn(Optional.of(user));
        assertEquals("c@bank.test", service.findByUsername("customer").email());
    }

    @Test
    void findByEmailIgnoresCase() {
        when(users.findByEmailIgnoreCase("C@BANK.TEST")).thenReturn(Optional.of(user));
        assertEquals(1L, service.findByEmail("C@BANK.TEST").id());
    }

    @Test
    void findByEmailThrowsWhenMissing() {
        when(users.findByEmailIgnoreCase("missing@bank.test")).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.findByEmail("missing@bank.test"));
    }

    @Test
    void updateChangesAllowedFields() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        UserResponse result = service.update(1L, new UpdateRequest(null, "Updated", null, null, false));
        assertEquals("Updated", result.fullName());
        assertFalse(result.enabled());
    }

    @Test
    void deleteDisablesUserInsteadOfRemovingData() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        service.delete(1L);
        assertFalse(user.isEnabled());
        verify(users, never()).delete(any());
    }
}
