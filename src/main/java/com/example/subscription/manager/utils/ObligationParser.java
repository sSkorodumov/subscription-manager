package com.example.subscription.manager.utils;

import com.example.subscription.manager.dto.ObligationResponse;
import com.example.subscription.manager.dto.RenewalAlertResponse;
import com.example.subscription.manager.models.Obligation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ObligationParser {
    public ObligationResponse parseObligation(Obligation obligation, String warning) {
        return  new ObligationResponse(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getAmount(),
                obligation.getCurrency(),
                obligation.getCategory(),
                obligation.getRecurrence(),
                obligation.getNextPaymentDate(),
                obligation.getStatus(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt(),
                warning
        );
    }

    public RenewalAlertResponse parseObligation(Obligation obligation) {
        return new RenewalAlertResponse(
                obligation.getId(),
                obligation.getTitle(),
                obligation.getNextPaymentDate(),
                obligation.getAmount(),
                obligation.getCurrency()
        );
    }
}
