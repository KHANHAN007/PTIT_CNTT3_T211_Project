package com.bankrestapi.controller;

import com.bankrestapi.audit.AuditedAction;
import com.bankrestapi.dto.ApiResponse;
import com.bankrestapi.dto.UserDtos.*;
import com.bankrestapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;

    @GetMapping
    public ApiResponse<Page<UserResponse>> list(@RequestParam(defaultValue = "") String search, Pageable pageable) {
        return ApiResponse.ok("Users retrieved", userService.list(search, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok("User retrieved", userService.findById(id));
    }

    @GetMapping("/by-username/{username}")
    public ApiResponse<UserResponse> findByUsername(@PathVariable String username) {
        return ApiResponse.ok("User retrieved", userService.findByUsername(username));
    }

    @GetMapping("/by-email")
    public ApiResponse<UserResponse> findByEmail(@RequestParam String email) {
        return ApiResponse.ok("User retrieved", userService.findByEmail(email));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AuditedAction("USER_CREATE")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok("User created", userService.register(request));
    }

    @PutMapping("/{id}")
    @AuditedAction("USER_UPDATE")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.ok("User updated", userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AuditedAction("USER_DISABLE")
    public void disable(@PathVariable Long id) {
        userService.delete(id);
    }
}
