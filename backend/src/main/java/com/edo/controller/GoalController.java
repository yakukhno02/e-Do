package com.edo.controller;

import com.edo.dto.request.GoalUpsertRequest;
import com.edo.dto.response.GoalProgressResponse;
import com.edo.security.UserPrincipal;
import com.edo.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalProgressResponse> upsertGoal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody GoalUpsertRequest request) {
        GoalProgressResponse response = goalService.upsertGoal(currentUser.getId(), request);
        return ResponseEntity.ok(response);
    }
}
