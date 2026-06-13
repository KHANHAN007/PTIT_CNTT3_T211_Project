package com.bankrestapi.service;

public interface LoginAttemptService {
    void failed(String username);
    void succeeded(String username);
}
