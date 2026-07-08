package com.example.subscription.manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "obligations", indexes = {
        @Index(name = "idx_obligation_title", columnList = "title")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Obligation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "title")
    private String title;

    @Column(nullable = false, scale = 2, precision = 19, name = "amount")
    private BigDecimal amount;

    @Column(nullable = false, name = "currency")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, name = "recurrence")
    private Recurrence recurrence;

    @Column(nullable = false, name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status")
    private Status status;

    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = true, name = "original_subscription_start_day")
    private Integer originalSubscriptionStartDay;

    @Builder
    public Obligation(String title, BigDecimal amount, String currency, Category category, Recurrence recurrence, LocalDate nextPaymentDate, Status status, Integer originalSubscriptionStartDay) {
        this.title = title;
        this.amount = amount;
        this.currency = currency;
        this.category = category;
        this.recurrence = recurrence;
        this.nextPaymentDate = nextPaymentDate;
        this.status = status;
        this.originalSubscriptionStartDay = originalSubscriptionStartDay;
    }

    public void expiry() {
        this.status = Status.EXPIRED;
    }

    public void shiftDay(LocalDate finalNextPaymentDate) {
        this.nextPaymentDate = finalNextPaymentDate;
    }

    public void cancel() {
        this.status = Status.CANCELLED;
    }
}
