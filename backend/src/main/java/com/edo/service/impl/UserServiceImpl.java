package com.edo.service.impl;

import com.edo.dto.request.UpdateProfileRequest;
import com.edo.dto.response.GoalProgressResponse;
import com.edo.dto.response.GoalsSummaryResponse;
import com.edo.dto.response.UserProfileResponse;
import com.edo.entity.ProductivityGoal;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.UserRepository;
import com.edo.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final ProductivityGoalRepository goalRepository;

    public UserServiceImpl(UserRepository userRepository,
                           PomodoroSessionRepository sessionRepository,
                           ProductivityGoalRepository goalRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.goalRepository = goalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        long completedPomodoros = sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long totalFocusMinutes = sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long completedSharedSessions = sessionRepository.countByUserIdAndTypeAndStatus(
                userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED
        );

        GoalsSummaryResponse goalsSummary = calculateGoalsProgress(userId);

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                completedPomodoros,
                totalFocusMinutes,
                completedSharedSessions,
                goalsSummary
        );
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ConflictException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        user.setName(request.getName());
        userRepository.save(user);

        return getProfile(userId);
    }

    private GoalsSummaryResponse calculateGoalsProgress(UUID userId) {
        List<ProductivityGoal> goals = goalRepository.findAllByUserId(userId);
        List<GoalProgressResponse> daily = new ArrayList<>();
        List<GoalProgressResponse> weekly = new ArrayList<>();

        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);

        Instant dayStart = nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);

        Instant weekStart = nowUtc.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        Instant weekEnd = weekStart.plusSeconds(7 * 86400);

        for (ProductivityGoal goal : goals) {
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

            GoalProgressResponse progressResponse = new GoalProgressResponse(
                    goal.getId(),
                    goal.getPeriod(),
                    goal.getType(),
                    goal.getTargetValue(),
                    currentValue,
                    progressPercentage
            );

            if (goal.getPeriod() == GoalPeriod.DAILY) {
                daily.add(progressResponse);
            } else {
                weekly.add(progressResponse);
            }
        }

        return new GoalsSummaryResponse(daily, weekly);
    }
}
