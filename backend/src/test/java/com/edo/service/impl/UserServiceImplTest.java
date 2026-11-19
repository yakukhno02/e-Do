package com.edo.service.impl;

import com.edo.dto.request.UpdateProfileRequest;
import com.edo.dto.response.GoalsSummaryResponse;
import com.edo.dto.response.UserProfileResponse;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.UserRole;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.UserRepository;
import com.edo.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @Mock
    private GoalService goalService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("user@example.com", "username", "Name", "hash", UserRole.USER);
        testUser.setId(userId);
    }

    @Test
    @DisplayName("getProfile aggregates user statistics and delegates goals summary")
    void getProfileSuccess() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(10L);
        when(sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED)).thenReturn(250L);
        when(sessionRepository.countByUserIdAndTypeAndStatus(userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED)).thenReturn(4L);
        when(goalService.getGoalsSummary(userId)).thenReturn(new GoalsSummaryResponse(List.of(), List.of()));

        UserProfileResponse response = userService.getProfile(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals(10L, response.getCompletedPomodoros());
        assertEquals(250L, response.getTotalFocusMinutes());
        assertEquals(4L, response.getCompletedSharedSessions());
        assertNotNull(response.getGoals());
        verify(goalService).getGoalsSummary(userId);
    }

    @Test
    @DisplayName("getProfile throws ResourceNotFoundException when user does not exist")
    void getProfileUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(userId));
    }

    @Test
    @DisplayName("updateProfile updates user name and username successfully")
    void updateProfileSuccess() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        request.setUsername("new_username");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("new_username")).thenReturn(false);
        when(goalService.getGoalsSummary(userId)).thenReturn(new GoalsSummaryResponse(List.of(), List.of()));

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertEquals("New Name", testUser.getName());
        assertEquals("new_username", testUser.getUsername());
        verify(userRepository).save(testUser);
        assertNotNull(response);
    }

    @Test
    @DisplayName("updateProfile throws ConflictException when new username is already taken")
    void updateProfileUsernameConflict() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("New Name");
        request.setUsername("taken_username");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("taken_username")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.updateProfile(userId, request));
        verify(userRepository, never()).save(any());
    }
}
