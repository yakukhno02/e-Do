package com.edo.controller;

import com.edo.dto.response.StatisticsSummaryResponse;
import com.edo.security.UserPrincipal;
import com.edo.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<StatisticsSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        StatisticsSummaryResponse response = statisticsService.getStatisticsSummary(currentUser.getId());
        return ResponseEntity.ok(response);
    }
}
