package com.bankrestapi.service;

import com.bankrestapi.model.BankTransaction;

public interface InterbankGateway {
    Result send(BankTransaction transaction);
    record Result(boolean accepted, boolean retryable, String message) {}
}
