package com.edo.service.impl;

import com.edo.dto.request.PomodoroStartRequest;
import com.edo.dto.response.PageResponse;
import com.edo.dto.response.PomodoroEndResponse;
import com.edo.dto.response.PomodoroStartResponse;
import com.edo.dto.response.SessionHistoryItem;
import com.edo.entity.PomodoroSession;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.UserRepository;
import com.edo.service.PomodoroService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PomodoroServiceImpl implements PomodoroService {

    private final PomodoroSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public PomodoroServiceImpl(PomodoroSessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PomodoroStartResponse startSession(UUID userId, PomodoroStartRequest request) {
        // Enforce single active session per user constraint
        if (sessionRepository.findByUserIdAndStatus(userId, PomodoroSessionStatus.IN_PROGRESS).isPresent()) {
            throw new ConflictException("User already has an active Pomodoro session");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        PomodoroSession session = new PomodoroSession(
                user,
                null,
                PomodoroSessionType.INDIVIDUAL,
                request.getDurationMinutes(),
                request.getBreakDurationMinutes(),
                Instant.now()
        );

        PomodoroSession savedSession = sessionRepository.save(session);

        return new PomodoroStartResponse(
                savedSession.getId(),
                savedSession.getType(),
                savedSession.getDurationMinutes(),
                savedSession.getBreakDurationMinutes(),
                savedSession.getStartTime(),
                savedSession.getStatus()
        );
    }

    @Override
    @Transactional
    public PomodoroEndResponse completeSession(UUID sessionId, UUID userId) {
        PomodoroSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pomodoro session not found with id: " + sessionId));

        if (session.getStatus() != PomodoroSessionStatus.IN_PROGRESS) {
            throw new ConflictException("Pomodoro session is not in progress");
        }

        session.setStatus(PomodoroSessionStatus.COMPLETED);
        session.setEndTime(Instant.now());

        PomodoroSession savedSession = sessionRepository.save(session);

        return new PomodoroEndResponse(
                savedSession.getId(),
                savedSession.getStatus(),
                savedSession.getEndTime(),
                savedSession.getDurationMinutes()
        );
    }

    @Override
    @Transactional
    public PomodoroEndResponse cancelSession(UUID sessionId, UUID userId) {
        PomodoroSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pomodoro session not found with id: " + sessionId));

        if (session.getStatus() != PomodoroSessionStatus.IN_PROGRESS) {
            throw new ConflictException("Pomodoro session is not in progress");
        }

        session.setStatus(PomodoroSessionStatus.CANCELLED);
        session.setEndTime(Instant.now());

        PomodoroSession savedSession = sessionRepository.save(session);

        return new PomodoroEndResponse(
                savedSession.getId(),
                savedSession.getStatus(),
                savedSession.getEndTime(),
                0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SessionHistoryItem> getSessionHistory(UUID userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startTime"), Sort.Order.desc("id")));
        Page<PomodoroSession> sessionPage = sessionRepository.findAllByUserId(userId, pageable);

        List<SessionHistoryItem> items = sessionPage.getContent().stream()
                .map(SessionHistoryItem::fromEntity)
                .toList();

        return new PageResponse<>(
                items,
                sessionPage.getNumber(),
                sessionPage.getSize(),
                sessionPage.getTotalElements(),
                sessionPage.getTotalPages()
        );
    }
}
