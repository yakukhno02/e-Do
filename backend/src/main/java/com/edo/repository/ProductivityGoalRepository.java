package com.edo.repository;

import com.edo.entity.ProductivityGoal;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductivityGoalRepository extends JpaRepository<ProductivityGoal, UUID> {
    Optional<ProductivityGoal> findByIdAndUserId(UUID id, UUID userId);
    Optional<ProductivityGoal> findByUserIdAndPeriodAndType(UUID userId, GoalPeriod period, GoalType type);
    List<ProductivityGoal> findAllByUserId(UUID userId);
}
