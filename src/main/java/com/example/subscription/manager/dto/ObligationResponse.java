package com.example.subscription.manager.dto;

import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Recurrence;
import com.example.subscription.manager.models.Status;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObligationResponse(
        @Schema(description = "Уникальный идентификатор (UUID)", example = "3fa85f64-5717-4582-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Название обязательства", example = "Netflix")
        String title,

        @Schema(description = "Сумма", example = "14.89")
        BigDecimal amount,

        @Schema(description = "Код валюты", example = "USD")
        String currency,

        @Schema(description = "Категория", example = "SUBSCRIPTION")
        Category category,

        @Schema(description = "Периодичность", example = "MONTHLY", nullable = true)
        Recurrence recurrence,

        @Schema(description = "Дата следующего платежа", example = "2026-08-06")
        LocalDate nextPaymentDate,

        @Schema(description = "Текущий статус", example = "ACTIVE")
        Status status,

        @Schema(description = "Дата и время создания", example = "2026-07-06T19:25:23")
        LocalDateTime createdAt,

        @Schema(description = "Дата и время обновления", example = "2026-07-06T19:25:23")
        LocalDateTime updatedAt,

        @Schema(description = "Предупреждение о дубликатах", example = "Активное обязательство с таким названием уже существует", nullable = true)
        String warning
) {}
