package com.edo.service.impl;

import com.edo.dto.request.UpdateProfileRequest;
import com.edo.dto.response.GoalsSummaryResponse;
import com.edo.dto.response.UserProfileResponse;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.UserRepository;
import com.edo.service.GoalService;
import com.edo.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final GoalService goalService;

    public UserServiceImpl(UserRepository userRepository,
                           PomodoroSessionRepository sessionRepository,
                           GoalService goalService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.goalService = goalService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        long completedPomodoros = sessionRepository.countByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long totalFocusMinutes = sessionRepository.sumDurationMinutesByUserIdAndStatus(userId, PomodoroSessionStatus.COMPLETED);
        long completedSharedSessions = sessionRepository.countByUserIdAndTypeAndStatus(
                userId, PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED
        );

        GoalsSummaryResponse goalsSummary = goalService.getGoalsSummary(userId);

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                completedPomodoros,
                totalFocusMinutes,
                completedSharedSessions,
                goalsSummary
        );
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ConflictException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        user.setName(request.getName());
        userRepository.save(user);

        return getProfile(userId);
    }
}
