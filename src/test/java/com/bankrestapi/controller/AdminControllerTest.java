package com.bankrestapi.controller;

import com.bankrestapi.dto.UserDtos.*;
import com.bankrestapi.model.Role;
import com.bankrestapi.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
    @Mock UserService service;
    AdminController controller;
    CreateRequest request;
    UserResponse response;

    @BeforeEach
    void setUp() {
        controller = new AdminController(service);
        request = new CreateRequest("user", "Password123", "u@bank.test", "User", "0902", Role.CUSTOMER);
        response = new UserResponse(1L, "user", "u@bank.test", "User", "0902",
                Role.CUSTOMER, true, false, LocalDateTime.now());
    }

    @Test
    void listReturnsPageEnvelope() {
        Pageable pageable = PageRequest.of(0, 10);
        when(service.list("", pageable)).thenReturn(new PageImpl<>(java.util.List.of(response)));
        assertEquals(1, controller.list("", pageable).data().getTotalElements());
    }

    @Test
    void createReturnsCreatedUser() {
        when(service.register(request)).thenReturn(response);
        assertEquals("user", controller.create(request).data().username());
    }

    @Test
    void findByIdReturnsUser() {
        when(service.findById(1L)).thenReturn(response);
        assertEquals("user", controller.findById(1L).data().username());
    }

    @Test
    void findByUsernameReturnsUser() {
        when(service.findByUsername("user")).thenReturn(response);
        assertEquals(1L, controller.findByUsername("user").data().id());
    }

    @Test
    void findByEmailReturnsUser() {
        when(service.findByEmail("u@bank.test")).thenReturn(response);
        assertEquals(1L, controller.findByEmail("u@bank.test").data().id());
    }

    @Test
    void updateReturnsUpdatedUser() {
        UpdateRequest update = new UpdateRequest(null, "Updated", null, null, null);
        when(service.update(1L, update)).thenReturn(response);
        assertTrue(controller.update(1L, update).success());
    }

    @Test
    void disableDelegatesToService() {
        controller.disable(1L);
        verify(service).delete(1L);
    }

    @Test
    void responsesUseStandardEnvelope() {
        when(service.register(request)).thenReturn(response);
        assertEquals("User created", controller.create(request).message());
    }
}
