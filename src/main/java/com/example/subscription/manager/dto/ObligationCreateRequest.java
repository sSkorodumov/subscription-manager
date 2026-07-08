package com.example.subscription.manager.dto;
import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Recurrence;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ObligationCreateRequest(
        @NotBlank(message = "Название не должно быть пустым")
        @Schema(description = "Название обязательства/подписки", example = "Netflix")
        String title,

        @NotNull(message = "Сумма обязательна")
        @Positive(message = "Сумма должна быть больше нуля")
        @Schema(description = "Сумма платежа", example = "14.89")
        BigDecimal amount,

        @NotBlank(message = "Валюта обязательна")
        @Size(min = 3, max = 3)
        @Schema(description = "Буквенный код валюты (ISO 4217)", example = "USD")
        String currency,

        @NotNull(message = "Категория обязательна")
        @Schema(description = "Категория расходов", example = "SUBSCRIPTION")
        Category category,

        @Schema(description = "Периодичность (null для разовых платежей)", example = "MONTHLY", nullable = true)
        Recurrence recurrence,

        @NotNull(message = "Дата следующего платежа обязательна")
        @Schema(description = "Дата платежа (гггг-мм-дд)", example = "2026-08-06")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate nextPaymentDate
) {}
