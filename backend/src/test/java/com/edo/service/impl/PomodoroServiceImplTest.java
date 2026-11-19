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
import com.edo.enums.UserRole;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PomodoroServiceImplTest {

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PomodoroServiceImpl pomodoroService;

    private User testUser;
    private UUID userId;
    private UUID sessionId;
    private PomodoroSession activeSession;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        testUser = new User("user@example.com", "username", "User Name", "passwordHash", UserRole.USER);
        testUser.setId(userId);

        activeSession = new PomodoroSession(testUser, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now());
        activeSession.setId(sessionId);
        activeSession.setStatus(PomodoroSessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("startSession creates and returns new session when user has no active sessions")
    void startSessionSuccess() {
        PomodoroStartRequest request = new PomodoroStartRequest();
        request.setDurationMinutes(25);
        request.setBreakDurationMinutes(5);

        when(sessionRepository.findByUserIdAndStatus(userId, PomodoroSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(sessionRepository.save(any(PomodoroSession.class))).thenAnswer(invocation -> {
            PomodoroSession session = invocation.getArgument(0);
            session.setId(sessionId);
            return session;
        });

        PomodoroStartResponse response = pomodoroService.startSession(userId, request);

        assertNotNull(response);
        assertEquals(sessionId, response.getSessionId());
        assertEquals(25, response.getDurationMinutes());
        assertEquals(PomodoroSessionStatus.IN_PROGRESS, response.getStatus());
        verify(sessionRepository).save(any(PomodoroSession.class));
    }

    @Test
    @DisplayName("startSession throws ConflictException when user already has an active session")
    void startSessionConflict() {
        PomodoroStartRequest request = new PomodoroStartRequest();
        when(sessionRepository.findByUserIdAndStatus(userId, PomodoroSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(activeSession));

        assertThrows(ConflictException.class, () -> pomodoroService.startSession(userId, request));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeSession completes in-progress session and sets duration")
    void completeSessionSuccess() {
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(activeSession));
        when(sessionRepository.save(any(PomodoroSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PomodoroEndResponse response = pomodoroService.completeSession(sessionId, userId);

        assertNotNull(response);
        assertEquals(PomodoroSessionStatus.COMPLETED, response.getStatus());
        assertEquals(25, response.getEarnedFocusMinutes());
        assertNotNull(response.getEndTime());
    }

    @Test
    @DisplayName("completeSession throws ConflictException when session is not in progress")
    void completeSessionNotActive() {
        activeSession.setStatus(PomodoroSessionStatus.COMPLETED);
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(activeSession));

        assertThrows(ConflictException.class, () -> pomodoroService.completeSession(sessionId, userId));
    }

    @Test
    @DisplayName("cancelSession cancels in-progress session with 0 earned focus minutes")
    void cancelSessionSuccess() {
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(activeSession));
        when(sessionRepository.save(any(PomodoroSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PomodoroEndResponse response = pomodoroService.cancelSession(sessionId, userId);

        assertNotNull(response);
        assertEquals(PomodoroSessionStatus.CANCELLED, response.getStatus());
        assertEquals(0, response.getEarnedFocusMinutes());
    }

    @Test
    @DisplayName("getSessionHistory validates page and size arguments")
    void getSessionHistoryInvalidPagination() {
        assertThrows(BadRequestException.class, () -> pomodoroService.getSessionHistory(userId, -1, 20));
        assertThrows(BadRequestException.class, () -> pomodoroService.getSessionHistory(userId, 0, 0));
        assertThrows(BadRequestException.class, () -> pomodoroService.getSessionHistory(userId, 0, 101));
    }

    @Test
    @DisplayName("getSessionHistory returns paginated session history")
    void getSessionHistorySuccess() {
        Page<PomodoroSession> page = new PageImpl<>(List.of(activeSession));
        when(sessionRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        PageResponse<SessionHistoryItem> result = pomodoroService.getSessionHistory(userId, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(sessionId, result.getContent().getFirst().getId());
    }
}
