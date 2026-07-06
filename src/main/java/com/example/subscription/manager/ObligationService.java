package com.example.subscription.manager;

import com.example.subscription.manager.models.Obligation;
import com.example.subscription.manager.models.Status;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;

@Service
@AllArgsConstructor
public class ObligationService {
    private final ObligationRepository obligationRepository;
    public CreateObligationReturnValue createObligation(ObligationCreateRequest request) {
        String warning = null;
        Obligation sameNameObligation = obligationRepository.findByTitleIgnoreCase(request.getTitle()).orElse(null);
        if (sameNameObligation != null && sameNameObligation.getStatus() == Status.ACTIVE) {
            warning = "Активное обязательство с таким названием уже существует";
        }
        Status calculatedStatus = request.getNextPaymentDate().isBefore(LocalDate.now())
                ? Status.EXPIRED
                : Status.ACTIVE;

        Obligation obligation = Obligation.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .category(request.getCategory())
                .recurrence(request.getRecurrence())
                .nextPaymentDate(request.getNextPaymentDate())
                .status(calculatedStatus)
                .originalSubscriptionStartDay(request.getNextPaymentDate().getDayOfMonth())
                .build();
        obligationRepository.save(obligation);
        return new CreateObligationReturnValue(new ObligationResponse(  obligation.getId(), obligation.getTitle(),
                                                                        obligation.getAmount(), obligation.getCurrency(),
                                                                        obligation.getCategory(), obligation.getRecurrence(),
                                                                        obligation.getNextPaymentDate(), obligation.getStatus()),
                                                                        warning);
    }
}
