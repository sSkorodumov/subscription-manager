package com.example.subscription.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Информация о зафиксированном платеже")
public record PaymentResponse(
        @Schema(description = "Уникальный идентификатор платежа (UUID)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID id,

        @JsonProperty("obligation_id")
        @Schema(description = "Идентификатор связанного обязательства (UUID)", example = "3fa85f64-5717-4582-b3fc-2c963f66afa6")
        UUID obligationId,

        @Schema(description = "Сумма проведенного платежа", example = "14.89")
        BigDecimal amount,

        @Schema(description = "Код валюты", example = "USD")
        String currency,

        @JsonProperty("paid_at")
        @Schema(description = "Дата и время фиксации оплаты", example = "2026-07-07T16:11:42")
        LocalDateTime paidAt
) {
}
