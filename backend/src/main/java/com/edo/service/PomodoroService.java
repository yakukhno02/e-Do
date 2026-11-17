package com.edo.service;

import com.edo.dto.request.PomodoroStartRequest;
import com.edo.dto.response.PageResponse;
import com.edo.dto.response.PomodoroEndResponse;
import com.edo.dto.response.PomodoroStartResponse;
import com.edo.dto.response.SessionHistoryItem;

import java.util.UUID;

public interface PomodoroService {
    PomodoroStartResponse startSession(UUID userId, PomodoroStartRequest request);
    PomodoroEndResponse completeSession(UUID sessionId, UUID userId);
    PomodoroEndResponse cancelSession(UUID sessionId, UUID userId);
    PageResponse<SessionHistoryItem> getSessionHistory(UUID userId, int page, int size);
}
