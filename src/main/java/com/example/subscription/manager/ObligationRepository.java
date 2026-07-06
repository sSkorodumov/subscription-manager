package com.example.subscription.manager;

import com.example.subscription.manager.models.Obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObligationRepository extends JpaRepository<Obligation, Long> {
    Optional<Obligation> findByTitleIgnoreCase(String title);
}
