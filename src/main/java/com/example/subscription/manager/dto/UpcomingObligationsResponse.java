package com.example.subscription.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpcomingObligationsResponse(
        List<ObligationResponse> obligations,

        Map<String, BigDecimal> totals,

        @JsonProperty("renewal_alerts")
        List<RenewalAlertResponse> renewalAlerts
) {
}
