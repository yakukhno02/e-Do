package com.edo.controller;

import com.edo.dto.request.PomodoroStartRequest;
import com.edo.dto.response.PageResponse;
import com.edo.dto.response.PomodoroEndResponse;
import com.edo.dto.response.PomodoroStartResponse;
import com.edo.dto.response.SessionHistoryItem;
import com.edo.security.UserPrincipal;
import com.edo.service.PomodoroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pomodoro")
public class PomodoroController {

    private final PomodoroService pomodoroService;

    public PomodoroController(PomodoroService pomodoroService) {
        this.pomodoroService = pomodoroService;
    }

    @PostMapping("/start")
    public ResponseEntity<PomodoroStartResponse> startSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PomodoroStartRequest request) {
        PomodoroStartResponse response = pomodoroService.startSession(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<PomodoroEndResponse> completeSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID sessionId) {
        PomodoroEndResponse response = pomodoroService.completeSession(sessionId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/cancel")
    public ResponseEntity<PomodoroEndResponse> cancelSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID sessionId) {
        PomodoroEndResponse response = pomodoroService.cancelSession(sessionId, currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<PageResponse<SessionHistoryItem>> getSessionHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<SessionHistoryItem> response = pomodoroService.getSessionHistory(currentUser.getId(), page, size);
        return ResponseEntity.ok(response);
    }
}
