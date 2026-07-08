package com.example.subscription.manager.dto;

import com.example.subscription.manager.models.Obligation;
import com.example.subscription.manager.models.Payment;

public record ServicePaymentResult(Obligation obligation, Payment payment) {}

