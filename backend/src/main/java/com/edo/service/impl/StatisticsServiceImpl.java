package com.edo.service.impl;

import com.edo.dto.response.SessionHistoryItem;
import com.edo.dto.response.StatisticsSummaryResponse;
import com.edo.entity.PomodoroSession;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.UserRepository;
import com.edo.service.StatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final PomodoroSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public StatisticsServiceImpl(PomodoroSessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsSummaryResponse getStatisticsSummary(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        long completedPomodoros = sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long totalFocusMinutes = sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long completedSharedSessions = sessionRepository.countByUserIdAndTypeAndStatus(
                userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED
        );

        // Fetch recent 10 sessions of current user, sorted by startTime DESC, id DESC
        Page<PomodoroSession> page = sessionRepository.findAllByUserId(
                userId,
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")))
        );

        List<SessionHistoryItem> recentSessions = page.getContent().stream()
                .map(SessionHistoryItem::fromEntity)
                .toList();

        return new StatisticsSummaryResponse(
                completedPomodoros,
                totalFocusMinutes,
                completedSharedSessions,
                recentSessions
        );
    }
}
