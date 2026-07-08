package com.example.subscription.manager.repositories;

import com.example.subscription.manager.models.Category;
import com.example.subscription.manager.models.Obligation;
import com.example.subscription.manager.models.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<Obligation, UUID> {
    boolean existsByTitleIgnoreCaseAndStatus(String title, Status status);
    List<Obligation> findAllByOrderByNextPaymentDateAsc();
    List<Obligation> findByStatusOrderByNextPaymentDateAsc(Status status);
    List<Obligation> findByCategoryOrderByNextPaymentDateAsc(Category category);
    List<Obligation> findByStatusAndCategoryOrderByNextPaymentDateAsc(Status status, Category category);
    List<Obligation> findByNextPaymentDateBetween(LocalDate start, LocalDate end);
}
