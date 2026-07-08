package com.example.subscription.manager.utils;

import com.example.subscription.manager.dto.PaymentResponse;
import com.example.subscription.manager.models.Payment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentParser {
    public PaymentResponse parsePayment(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getObligation().getId(), payment.getAmount(), payment.getCurrency(), payment.getPaidAt());
    }
}
