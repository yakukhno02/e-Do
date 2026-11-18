package com.edo.dto.response;

import java.util.List;

public class StatisticsSummaryResponse {

    private long totalCompletedPomodoros;
    private long totalFocusMinutes;
    private long totalSharedSessions;
    private List<SessionHistoryItem> recentSessions;

    public StatisticsSummaryResponse() {
    }

    public StatisticsSummaryResponse(long totalCompletedPomodoros, long totalFocusMinutes,
                                     long totalSharedSessions, List<SessionHistoryItem> recentSessions) {
        this.totalCompletedPomodoros = totalCompletedPomodoros;
        this.totalFocusMinutes = totalFocusMinutes;
        this.totalSharedSessions = totalSharedSessions;
        this.recentSessions = recentSessions;
    }

    public long getTotalCompletedPomodoros() {
        return totalCompletedPomodoros;
    }

    public void setTotalCompletedPomodoros(long totalCompletedPomodoros) {
        this.totalCompletedPomodoros = totalCompletedPomodoros;
    }

    public long getTotalFocusMinutes() {
        return totalFocusMinutes;
    }

    public void setTotalFocusMinutes(long totalFocusMinutes) {
        this.totalFocusMinutes = totalFocusMinutes;
    }

    public long getTotalSharedSessions() {
        return totalSharedSessions;
    }

    public void setTotalSharedSessions(long totalSharedSessions) {
        this.totalSharedSessions = totalSharedSessions;
    }

    public List<SessionHistoryItem> getRecentSessions() {
        return recentSessions;
    }

    public void setRecentSessions(List<SessionHistoryItem> recentSessions) {
        this.recentSessions = recentSessions;
    }
}
