package com.example.subscription.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;


public record RenewalAlertResponse(@Schema(description = "Уникальный идентификатор (UUID)", example = "3fa85f64-5717-4582-b3fc-2c963f66afa6")
                                   UUID id,

                                   @Schema(description = "Название обязательства", example = "Netflix")
                                   String title,

                                   @JsonProperty("next_payment_date")
                                   @Schema(description = "Дата следующего платежа", example = "2026-08-06")
                                   LocalDate nextPaymentDate,

                                   @Schema(description = "Сумма", example = "14.89")
                                   BigDecimal amount,

                                   @Schema(description = "Код валюты", example = "USD")
                                   String currency) {
}
