package com.edo.entity;

import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "productivity_goals",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_period_type", columnNames = {"user_id", "period", "type"})
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ProductivityGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    private GoalPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private GoalType type;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProductivityGoal() {
    }

    public ProductivityGoal(User user, GoalPeriod period, GoalType type, int targetValue) {
        this.user = user;
        this.period = period;
        this.type = type;
        this.targetValue = targetValue;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GoalPeriod getPeriod() {
        return period;
    }

    public void setPeriod(GoalPeriod period) {
        this.period = period;
    }

    public GoalType getType() {
        return type;
    }

    public void setType(GoalType type) {
        this.type = type;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
