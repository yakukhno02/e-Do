package com.edo.service.impl;

import com.edo.dto.request.GoalUpsertRequest;
import com.edo.dto.response.GoalProgressResponse;
import com.edo.dto.response.GoalsSummaryResponse;
import com.edo.entity.ProductivityGoal;
import com.edo.entity.User;
import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import com.edo.enums.UserRole;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private ProductivityGoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @InjectMocks
    private GoalServiceImpl goalService;

    private User testUser;
    private UUID userId;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();
        testUser = new User("user@example.com", "username", "Name", "hash", UserRole.USER);
        testUser.setId(userId);
    }

    @Test
    @DisplayName("upsertGoal creates new goal when goal does not exist")
    void upsertGoalCreatesNew() {
        GoalUpsertRequest request = new GoalUpsertRequest();
        request.setPeriod(GoalPeriod.DAILY);
        request.setType(GoalType.POMODORO_COUNT);
        request.setTargetValue(8);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(goalRepository.findByUserIdAndPeriodAndType(userId, GoalPeriod.DAILY, GoalType.POMODORO_COUNT))
                .thenReturn(Optional.empty());

        when(goalRepository.save(any(ProductivityGoal.class))).thenAnswer(inv -> {
            ProductivityGoal g = inv.getArgument(0);
            g.setId(goalId);
            return g;
        });

        when(sessionRepository.countCompletedInPeriod(eq(userId), any(), any())).thenReturn(4L);

        GoalProgressResponse response = goalService.upsertGoal(userId, request);

        assertNotNull(response);
        assertEquals(8, response.getTargetValue());
        assertEquals(4, response.getCurrentValue());
        assertEquals(50.0, response.getProgressPercentage(), 0.001);
        verify(goalRepository).save(any(ProductivityGoal.class));
    }

    @Test
    @DisplayName("upsertGoal updates existing goal target value")
    void upsertGoalUpdatesExisting() {
        GoalUpsertRequest request = new GoalUpsertRequest();
        request.setPeriod(GoalPeriod.WEEKLY);
        request.setType(GoalType.FOCUS_MINUTES);
        request.setTargetValue(2400);

        ProductivityGoal existingGoal = new ProductivityGoal(testUser, GoalPeriod.WEEKLY, GoalType.FOCUS_MINUTES, 1200);
        existingGoal.setId(goalId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(goalRepository.findByUserIdAndPeriodAndType(userId, GoalPeriod.WEEKLY, GoalType.FOCUS_MINUTES))
                .thenReturn(Optional.of(existingGoal));
        when(goalRepository.save(existingGoal)).thenReturn(existingGoal);
        when(sessionRepository.sumMinutesInPeriod(eq(userId), any(), any())).thenReturn(1200L);

        GoalProgressResponse response = goalService.upsertGoal(userId, request);

        assertEquals(2400, response.getTargetValue());
        assertEquals(1200, response.getCurrentValue());
        assertEquals(50.0, response.getProgressPercentage(), 0.001);
    }

    @Test
    @DisplayName("upsertGoal throws ResourceNotFoundException when user not found")
    void upsertGoalUserNotFound() {
        GoalUpsertRequest request = new GoalUpsertRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.upsertGoal(userId, request));
    }

    @Test
    @DisplayName("getGoalsSummary separates daily and weekly goals correctly")
    void getGoalsSummarySuccess() {
        ProductivityGoal dailyGoal = new ProductivityGoal(testUser, GoalPeriod.DAILY, GoalType.POMODORO_COUNT, 5);
        dailyGoal.setId(UUID.randomUUID());
        ProductivityGoal weeklyGoal = new ProductivityGoal(testUser, GoalPeriod.WEEKLY, GoalType.FOCUS_MINUTES, 1000);
        weeklyGoal.setId(UUID.randomUUID());

        when(goalRepository.findAllByUserId(userId)).thenReturn(List.of(dailyGoal, weeklyGoal));
        when(sessionRepository.countCompletedInPeriod(eq(userId), any(), any())).thenReturn(5L);
        when(sessionRepository.sumMinutesInPeriod(eq(userId), any(), any())).thenReturn(500L);

        GoalsSummaryResponse summary = goalService.getGoalsSummary(userId);

        assertEquals(1, summary.getDaily().size());
        assertEquals(1, summary.getWeekly().size());
        assertEquals(100.0, summary.getDaily().getFirst().getProgressPercentage(), 0.001);
        assertEquals(50.0, summary.getWeekly().getFirst().getProgressPercentage(), 0.001);
    }
}
