package com.example.subscription.manager.dto;

public record PaymentResultResponse(
        ObligationResponse obligation,
        PaymentResponse payment
) {}
