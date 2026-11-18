package com.edo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RoomCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Pomodoro duration is required")
    @Min(value = 1, message = "Pomodoro duration must be between 1 and 120 minutes")
    @Max(value = 120, message = "Pomodoro duration must be between 1 and 120 minutes")
    private Integer pomodoroDuration;

    @NotNull(message = "Break duration is required")
    @Min(value = 1, message = "Break duration must be between 1 and 60 minutes")
    @Max(value = 60, message = "Break duration must be between 1 and 60 minutes")
    private Integer breakDuration;

    public RoomCreateRequest() {
    }

    public RoomCreateRequest(String name, Integer pomodoroDuration, Integer breakDuration) {
        this.name = name;
        this.pomodoroDuration = pomodoroDuration;
        this.breakDuration = breakDuration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPomodoroDuration() {
        return pomodoroDuration;
    }

    public void setPomodoroDuration(Integer pomodoroDuration) {
        this.pomodoroDuration = pomodoroDuration;
    }

    public Integer getBreakDuration() {
        return breakDuration;
    }

    public void setBreakDuration(Integer breakDuration) {
        this.breakDuration = breakDuration;
    }
}
