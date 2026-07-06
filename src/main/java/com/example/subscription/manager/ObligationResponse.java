package com.example.subscription.manager;

import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Recurrence;
import com.example.subscription.manager.models.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ObligationResponse(Long id, String title, BigDecimal amount, String currency, Category category, Recurrence recurrence,
                                 LocalDate nextPaymentDate, Status status) {
}
