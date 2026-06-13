package com.bankrestapi.service.impl;

import com.bankrestapi.service.*;

import com.bankrestapi.model.BankTransaction;
import org.springframework.stereotype.Service;

@Service
public class SimulatedInterbankGateway implements InterbankGateway {
    @Override
    public Result send(BankTransaction transaction) {
        String bank = transaction.getExternalBankCode();
        if ("TIMEOUT".equalsIgnoreCase(bank)) return new Result(false, true, "Partner bank timeout");
        if ("REJECT".equalsIgnoreCase(bank)) return new Result(false, false, "Partner bank rejected transfer");
        return new Result(true, false, "Accepted by partner bank");
    }
}
