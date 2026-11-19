package com.edo.service.impl;

import com.edo.dto.response.StatisticsSummaryResponse;
import com.edo.entity.PomodoroSession;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.UserRole;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getStatisticsSummary returns aggregated data for existing user")
    void getStatisticsSummarySuccess() {
        User user = new User("user@example.com", "user", "User", "hash", UserRole.USER);
        user.setId(userId);

        PomodoroSession session = new PomodoroSession(user, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now());
        session.setId(UUID.randomUUID());
        session.setStatus(PomodoroSessionStatus.COMPLETED);

        Page<PomodoroSession> page = new PageImpl<>(List.of(session));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(10L);
        when(sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(250L);
        when(sessionRepository.countByUserIdAndTypeAndStatus(userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED)).thenReturn(3L);
        when(sessionRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        StatisticsSummaryResponse response = statisticsService.getStatisticsSummary(userId);

        assertNotNull(response);
        assertEquals(10L, response.getTotalCompletedPomodoros());
        assertEquals(250L, response.getTotalFocusMinutes());
        assertEquals(3L, response.getTotalSharedSessions());
        assertEquals(1, response.getRecentSessions().size());
    }

    @Test
    @DisplayName("getStatisticsSummary throws ResourceNotFoundException when user does not exist")
    void getStatisticsSummaryUserNotFound() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> statisticsService.getStatisticsSummary(userId));
    }
}
