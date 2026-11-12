package com.edo.dto.response;

import com.edo.enums.GoalPeriod;
import com.edo.enums.GoalType;

import java.util.UUID;

public class GoalProgressResponse {

    private UUID id;
    private GoalPeriod period;
    private GoalType type;
    private int targetValue;
    private int currentValue;
    private double progressPercentage;

    public GoalProgressResponse() {
    }

    public GoalProgressResponse(UUID id, GoalPeriod period, GoalType type, int targetValue, int currentValue, double progressPercentage) {
        this.id = id;
        this.period = period;
        this.type = type;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.progressPercentage = progressPercentage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public int getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
}
