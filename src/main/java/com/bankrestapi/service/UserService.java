package com.bankrestapi.service;

import com.bankrestapi.dto.UserDtos.CreateRequest;
import com.bankrestapi.dto.UserDtos.UpdateRequest;
import com.bankrestapi.dto.UserDtos.UserResponse;
import com.bankrestapi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse register(CreateRequest request);
    UserResponse registerCustomer(CreateRequest request);
    Page<UserResponse> list(String search, Pageable pageable);
    UserResponse findById(Long id);
    UserResponse findByUsername(String username);
    UserResponse findByEmail(String email);
    UserResponse update(Long id, UpdateRequest request);
    UserResponse staffUpdate(Long id, UpdateRequest request);
    void delete(Long id);
    User get(Long id);
    User getByUsername(String username);
}
