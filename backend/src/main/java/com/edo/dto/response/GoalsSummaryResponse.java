package com.edo.dto.response;

import java.util.ArrayList;
import java.util.List;

public class GoalsSummaryResponse {

    private List<GoalProgressResponse> daily = new ArrayList<>();
    private List<GoalProgressResponse> weekly = new ArrayList<>();

    public GoalsSummaryResponse() {
    }

    public GoalsSummaryResponse(List<GoalProgressResponse> daily, List<GoalProgressResponse> weekly) {
        this.daily = daily != null ? daily : new ArrayList<>();
        this.weekly = weekly != null ? weekly : new ArrayList<>();
    }

    public List<GoalProgressResponse> getDaily() {
        return daily;
    }

    public void setDaily(List<GoalProgressResponse> daily) {
        this.daily = daily;
    }

    public List<GoalProgressResponse> getWeekly() {
        return weekly;
    }

    public void setWeekly(List<GoalProgressResponse> weekly) {
        this.weekly = weekly;
    }
}
