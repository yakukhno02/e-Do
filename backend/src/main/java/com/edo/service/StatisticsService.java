package com.edo.service;

import com.edo.dto.response.StatisticsSummaryResponse;

import java.util.UUID;

public interface StatisticsService {
    StatisticsSummaryResponse getStatisticsSummary(UUID userId);
}
