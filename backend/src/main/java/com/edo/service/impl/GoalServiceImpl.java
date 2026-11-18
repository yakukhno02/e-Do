package com.edo.service.impl;

import com.edo.dto.request.GoalUpsertRequest;
import com.edo.dto.response.GoalProgressResponse;
import com.edo.dto.response.GoalsSummaryResponse;
import com.edo.entity.ProductivityGoal;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.UserRepository;
import com.edo.service.GoalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoalServiceImpl implements GoalService {

    private final ProductivityGoalRepository goalRepository;
    private final UserRepository userRepository;
    private final PomodoroSessionRepository sessionRepository;

    public GoalServiceImpl(ProductivityGoalRepository goalRepository,
                           UserRepository userRepository,
                           PomodoroSessionRepository sessionRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public GoalProgressResponse upsertGoal(UUID userId, GoalUpsertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Optional<ProductivityGoal> existingGoal = goalRepository.findByUserIdAndPeriodAndType(
                userId, request.getPeriod(), request.getType()
        );

        ProductivityGoal goal;
        if (existingGoal.isPresent()) {
            goal = existingGoal.get();
            goal.setTargetValue(request.getTargetValue());
        } else {
            goal = new ProductivityGoal(user, request.getPeriod(), request.getType(), request.getTargetValue());
        }

        ProductivityGoal savedGoal = goalRepository.save(goal);

        return calculateProgress(userId, savedGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalsSummaryResponse getGoalsSummary(UUID userId) {
        List<ProductivityGoal> goals = goalRepository.findAllByUserId(userId);
        List<GoalProgressResponse> daily = new ArrayList<>();
        List<GoalProgressResponse> weekly = new ArrayList<>();

        for (ProductivityGoal goal : goals) {
            GoalProgressResponse progressResponse = calculateProgress(userId, goal);
            if (goal.getPeriod() == GoalPeriod.DAILY) {
                daily.add(progressResponse);
            } else {
                weekly.add(progressResponse);
            }
        }

        return new GoalsSummaryResponse(daily, weekly);
    }

    private GoalProgressResponse calculateProgress(UUID userId, ProductivityGoal goal) {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);

        Instant dayStart = nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);

        Instant weekStart = nowUtc.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant weekEnd = weekStart.plusSeconds(7 * 86400);

        Instant start = goal.getPeriod() == GoalPeriod.DAILY ? dayStart : weekStart;
        Instant end = goal.getPeriod() == GoalPeriod.DAILY ? dayEnd : weekEnd;

        int currentValue;
        if (goal.getType() == GoalType.POMODORO_COUNT) {
            currentValue = (int) sessionRepository.countCompletedInPeriod(userId, start, end);
        } else {
            currentValue = (int) sessionRepository.sumMinutesInPeriod(userId, start, end);
        }

        double progressPercentage = 0.0;
        if (goal.getTargetValue() > 0) {
            progressPercentage = ((double) currentValue / goal.getTargetValue()) * 100.0;
        }

        return new GoalProgressResponse(
                goal.getId(),
                goal.getPeriod(),
                goal.getType(),
                goal.getTargetValue(),
                currentValue,
                progressPercentage
        );
    }
}
