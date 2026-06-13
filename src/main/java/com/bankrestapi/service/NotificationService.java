package com.bankrestapi.service;

public interface NotificationService {
    void send(String email, String subject, String body);
}
