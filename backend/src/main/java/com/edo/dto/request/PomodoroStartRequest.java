package com.edo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PomodoroStartRequest {

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be between 1 and 120 minutes")
    @Max(value = 120, message = "Duration must be between 1 and 120 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Break duration is required")
    @Min(value = 1, message = "Break duration must be between 1 and 60 minutes")
    @Max(value = 60, message = "Break duration must be between 1 and 60 minutes")
    private Integer breakDurationMinutes;

    public PomodoroStartRequest() {
    }

    public PomodoroStartRequest(Integer durationMinutes, Integer breakDurationMinutes) {
        this.durationMinutes = durationMinutes;
        this.breakDurationMinutes = breakDurationMinutes;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getBreakDurationMinutes() {
        return breakDurationMinutes;
    }

    public void setBreakDurationMinutes(Integer breakDurationMinutes) {
        this.breakDurationMinutes = breakDurationMinutes;
    }
}
