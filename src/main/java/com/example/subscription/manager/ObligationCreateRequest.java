package com.example.subscription.manager;

import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Recurrence;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class ObligationCreateRequest {
    @NotBlank
    private String title;

    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private Category category;

    private Recurrence recurrence;

    @NotNull
    private LocalDate nextPaymentDate;
}
