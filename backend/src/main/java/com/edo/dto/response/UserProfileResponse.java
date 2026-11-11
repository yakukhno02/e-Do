package com.edo.dto.response;

import java.util.UUID;

public class UserProfileResponse {

    private UUID id;
    private String name;
    private String username;
    private String email;
    private String role;
    private long completedPomodoros;
    private long totalFocusMinutes;
    private long completedSharedSessions;
    private GoalsSummaryResponse goals;

    public UserProfileResponse() {
    }

    public UserProfileResponse(UUID id, String name, String username, String email, String role,
                               long completedPomodoros, long totalFocusMinutes, long completedSharedSessions,
                               GoalsSummaryResponse goals) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.completedPomodoros = completedPomodoros;
        this.totalFocusMinutes = totalFocusMinutes;
        this.completedSharedSessions = completedSharedSessions;
        this.goals = goals;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getCompletedPomodoros() {
        return completedPomodoros;
    }

    public void setCompletedPomodoros(long completedPomodoros) {
        this.completedPomodoros = completedPomodoros;
    }

    public long getTotalFocusMinutes() {
        return totalFocusMinutes;
    }

    public void setTotalFocusMinutes(long totalFocusMinutes) {
        this.totalFocusMinutes = totalFocusMinutes;
    }

    public long getCompletedSharedSessions() {
        return completedSharedSessions;
    }

    public void setCompletedSharedSessions(long completedSharedSessions) {
        this.completedSharedSessions = completedSharedSessions;
    }

    public GoalsSummaryResponse getGoals() {
        return goals;
    }

    public void setGoals(GoalsSummaryResponse goals) {
        this.goals = goals;
    }
}
