package com.example.subscription.manager.dto;

import com.example.subscription.manager.models.Obligation;

public record CreateObligationReturnValue(Obligation obligation, String warning) {
}
