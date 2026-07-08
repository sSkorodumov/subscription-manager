package com.example.subscription.manager.services;

import com.example.subscription.manager.dto.CreateObligationReturnValue;
import com.example.subscription.manager.dto.ObligationCreateRequest;
import com.example.subscription.manager.dto.ServicePaymentResult;
import com.example.subscription.manager.models.*;
import com.example.subscription.manager.repositories.ObligationRepository;
import com.example.subscription.manager.repositories.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@AllArgsConstructor
public class ObligationService {
    private final ObligationRepository obligationRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public CreateObligationReturnValue createObligation(ObligationCreateRequest request) {
        String warning = null;
        if (obligationRepository.existsByTitleIgnoreCaseAndStatus(request.title(), Status.ACTIVE)){
            warning = "Активное обязательство с таким названием уже существует";
        }
        Status calculatedStatus = request.nextPaymentDate().isBefore(LocalDate.now())
                ? Status.EXPIRED
                : Status.ACTIVE;
        Integer startDay = (request.recurrence() != null)
                ? request.nextPaymentDate().getDayOfMonth()
                : null;

        Obligation obligation = Obligation.builder()
                .title(request.title())
                .amount(request.amount())
                .currency(request.currency())
                .category(request.category())
                .recurrence(request.recurrence())
                .nextPaymentDate(request.nextPaymentDate())
                .status(calculatedStatus)
                .originalSubscriptionStartDay(startDay)
                .build();
        Obligation savedObligation = obligationRepository.save(obligation);
        return new CreateObligationReturnValue(savedObligation, warning);
    }

    @Transactional
    public List<Obligation> getObligations(Category category, Status status) {
        List<Obligation> returnList = new ArrayList<>();
        if (category != null && status != null) {
            returnList = obligationRepository.findByStatusAndCategoryOrderByNextPaymentDateAsc(status, category);
        } else if (category != null) {
            returnList = obligationRepository.findByCategoryOrderByNextPaymentDateAsc(category);
        } else if (status != null) {
            returnList = obligationRepository.findByStatusOrderByNextPaymentDateAsc(status);
        } else {
            returnList = obligationRepository.findAllByOrderByNextPaymentDateAsc();
        }
        List<Obligation> obligationsToUpdate = new ArrayList<>();
        for (Obligation obligation : returnList) {
            if (obligation.getRecurrence() == null && obligation.getStatus() == Status.ACTIVE && obligation.getNextPaymentDate().isBefore(LocalDate.now())) {
                obligation.expiry();
                obligationsToUpdate.add(obligation);
            }
        }
        if (!obligationsToUpdate.isEmpty()) {
            obligationRepository.saveAll(obligationsToUpdate);
        }
        return returnList;
    }

    public List<Obligation> getUpcomingObligations(Integer days) {
        return obligationRepository.findByNextPaymentDateBetween(LocalDate.now(), LocalDate.now().plusDays(days));
    }

    public Map<String, BigDecimal> countTotals(List<Obligation> obligations) {
        var totals = new HashMap<String, BigDecimal>();
        for (Obligation obligation : obligations) {
            if (totals.containsKey(obligation.getCurrency())) {
                totals.computeIfPresent(obligation.getCurrency(), (key, oldValue) -> oldValue.add(obligation.getAmount()));
            } else {
                totals.put(obligation.getCurrency(), obligation.getAmount());
            }
        }
        return totals;
    }

    public List<Obligation> getUpcomingSubscriptions(List<Obligation> obligations) {
        List<Obligation> subscriptions = new ArrayList<>();
        for (Obligation obligation : obligations) {
            if (obligation.getRecurrence() != null && obligation.getCategory() == Category.SUBSCRIPTION) {
                subscriptions.add(obligation);
            }
        }
        return subscriptions;
    }

    @Transactional
    public ServicePaymentResult pay(UUID id) {
        Obligation checkToPay = obligationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обязательство не найдено"));
        if (checkToPay.getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Оплата возможна только для активных обязательств");
        }
        Payment payment = Payment.builder().obligation(checkToPay).amount(checkToPay.getAmount()).currency(checkToPay.getCurrency()).build();
        Payment savedPayment = paymentRepository.save(payment);

        if (checkToPay.getRecurrence() != null) {
            LocalDate finalNextPaymentDate = getLocalDate(checkToPay);
            checkToPay.shiftDay(finalNextPaymentDate);
            Obligation savedObligation = obligationRepository.save(checkToPay);
            return new ServicePaymentResult(savedObligation, savedPayment);
        } else {
            checkToPay.cancel();
            Obligation savedObligation = obligationRepository.save(checkToPay);
            return new ServicePaymentResult(savedObligation, savedPayment);
        }
    }

    private LocalDate getLocalDate(Obligation checkToPay) {
        LocalDate baseNext = checkToPay.getNextPaymentDate();
        switch (checkToPay.getRecurrence()) {
            case MONTHLY:
                baseNext = baseNext.plusMonths(1);
                break;
            case QUARTERLY:
                baseNext = baseNext.plusMonths(3);
                break;
            case YEARLY:
                baseNext = baseNext.plusYears(1);
                break;
        }
        int lengthOfFieldMonth = baseNext.lengthOfMonth();
        int targetDay = Math.min(checkToPay.getOriginalSubscriptionStartDay(), lengthOfFieldMonth);
        return baseNext.withDayOfMonth(targetDay);
    }

    @Transactional
    public Obligation cancel(UUID id) {
        Obligation obligationToCancel = obligationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обязательство не найдено"));
        if (obligationToCancel.getStatus() != Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Отмена возможна только для активных обязательств");
        }
        obligationToCancel.cancel();
        return obligationRepository.save(obligationToCancel);
    }

    @Transactional
    public void delete(UUID id) {
        Obligation obligationToDelete = obligationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Обязательство не найдено"));
        obligationRepository.delete(obligationToDelete);
    }
}
