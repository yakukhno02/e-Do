package com.edo.dto.request;

import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GoalUpsertRequest {

    @NotNull(message = "Period is required")
    private GoalPeriod period;

    @NotNull(message = "Type is required")
    private GoalType type;

    @NotNull(message = "Target value is required")
    @Min(value = 1, message = "Target value must be at least 1")
    private Integer targetValue;

    public GoalUpsertRequest() {
    }

    public GoalUpsertRequest(GoalPeriod period, GoalType type, Integer targetValue) {
        this.period = period;
        this.type = type;
        this.targetValue = targetValue;
    }

    public GoalPeriod getPeriod() {
        return period;
    }

    public void setPeriod(GoalPeriod period) {
        this.period = period;
    }

    public GoalType getType() {
        return type;
    }

    public void setType(GoalType type) {
        this.type = type;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }
}
