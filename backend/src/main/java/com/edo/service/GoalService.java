package com.edo.service;

import com.edo.dto.request.GoalUpsertRequest;
import com.edo.dto.response.GoalProgressResponse;
import com.edo.dto.response.GoalsSummaryResponse;

import java.util.UUID;

public interface GoalService {
    GoalProgressResponse upsertGoal(UUID userId, GoalUpsertRequest request);
    GoalsSummaryResponse getGoalsSummary(UUID userId);
}
