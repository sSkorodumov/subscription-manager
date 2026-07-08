package com.example.subscription.manager.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obligation_id", nullable = false)
    private Obligation obligation;

    @Column(nullable = false, scale = 2, precision = 19, name = "amount")
    private BigDecimal amount;

    @Column(nullable = false, name = "currency")
    private String currency;

    @CreationTimestamp
    @Column(nullable = false, updatable = false, name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    public Payment(Obligation obligation, BigDecimal amount, String currency) {
        this.obligation = obligation;
        this.amount = amount;
        this.currency = currency;
    }
}
