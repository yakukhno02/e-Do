package com.edo.service.impl;

import com.edo.dto.response.AdminRoomItem;
import com.edo.dto.response.AdminUserItem;
import com.edo.dto.response.PageResponse;
import com.edo.entity.Room;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.exception.BadRequestException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PasswordResetTokenRepository;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.ProductivityGoalRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.TaskRepository;
import com.edo.repository.UserRepository;
import com.edo.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final ProductivityGoalRepository goalRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            TaskRepository taskRepository,
                            PomodoroSessionRepository sessionRepository,
                            ProductivityGoalRepository goalRepository,
                            RoomRepository roomRepository,
                            RoomParticipantRepository roomParticipantRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.sessionRepository = sessionRepository;
        this.goalRepository = goalRepository;
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserItem> getUsers(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<User> userPage = userRepository.findAll(pageable);

        List<AdminUserItem> items = userPage.getContent().stream()
                .map(user -> {
                    long completedPomodoros = sessionRepository.countByUserIdAndStatus(
                            user.getId(), PomodoroSessionStatus.COMPLETED
                    );
                    long totalFocusMinutes = sessionRepository.sumDurationMinutesByUserIdAndStatus(
                            user.getId(), PomodoroSessionStatus.COMPLETED
                    );
                    long completedSharedSessions = sessionRepository.countByUserIdAndTypeAndStatus(
                            user.getId(), PomodoroSessionType.SHARED, PomodoroSessionStatus.COMPLETED
                    );

                    return new AdminUserItem(
                            user.getId(),
                            user.getName(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRole().name(),
                            user.getCreatedAt(),
                            completedPomodoros,
                            totalFocusMinutes,
                            completedSharedSessions
                    );
                })
                .toList();

        return new PageResponse<>(items, userPage.getNumber(), userPage.getSize(), userPage.getTotalElements(), userPage.getTotalPages());
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Delete rooms created by this user
        List<Room> createdRooms = roomRepository.findAllByCreatorId(userId);
        for (Room room : createdRooms) {
            sessionRepository.setRoomNullByRoomId(room.getId());
            roomRepository.delete(room);
        }

        roomParticipantRepository.deleteAllByUserId(userId);
        sessionRepository.deleteAllByUserId(userId);
        goalRepository.deleteAllByUserId(userId);
        taskRepository.deleteAllByUserId(userId);
        passwordResetTokenRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRoomItem> getRooms(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Room> roomPage = roomRepository.findAll(pageable);

        List<AdminRoomItem> items = roomPage.getContent().stream()
                .map(room -> {
                    int participantsCount = roomParticipantRepository.countByRoomId(room.getId());
                    return new AdminRoomItem(
                            room.getId(),
                            room.getCode(),
                            room.getName(),
                            room.getCreator().getId(),
                            room.getStatus(),
                            room.getPomodoroDuration(),
                            room.getBreakDuration(),
                            room.getCreatedAt(),
                            participantsCount
                    );
                })
                .toList();

        return new PageResponse<>(items, roomPage.getNumber(), roomPage.getSize(), roomPage.getTotalElements(), roomPage.getTotalPages());
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        sessionRepository.setRoomNullByRoomId(roomId);
        roomRepository.delete(room);
    }
}
