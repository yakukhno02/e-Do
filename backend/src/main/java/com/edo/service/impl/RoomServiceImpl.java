package com.edo.service.impl;

import com.edo.dto.request.RoomCreateRequest;
import com.edo.dto.request.RoomJoinRequest;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RoomDetailsResponse;
import com.edo.dto.response.RoomParticipantResponse;
import com.edo.dto.websocket.RoomUpdatedEvent;
import com.edo.entity.PomodoroSession;
import com.edo.entity.Room;
import com.edo.entity.RoomParticipant;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.RoomStatus;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.UserRepository;
import com.edo.service.RoomService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RoomServiceImpl implements RoomService {

    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern ROOM_CODE_PATTERN = Pattern.compile("^[A-HJ-NP-Z2-9]{6}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final PomodoroSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final Optional<SimpMessagingTemplate> messagingTemplate;

    public RoomServiceImpl(RoomRepository roomRepository,
                           RoomParticipantRepository roomParticipantRepository,
                           PomodoroSessionRepository sessionRepository,
                           UserRepository userRepository,
                           Optional<SimpMessagingTemplate> messagingTemplate) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public RoomDetailsResponse createRoom(UUID userId, RoomCreateRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String code = generateUniqueRoomCode();

        Room room = new Room(
                code,
                request.getName(),
                creator,
                request.getPomodoroDuration(),
                request.getBreakDuration()
        );

        Room savedRoom = roomRepository.save(room);

        // Creator automatically joins as the first participant
        RoomParticipant creatorParticipant = new RoomParticipant(savedRoom, creator);
        roomParticipantRepository.save(creatorParticipant);

        RoomDetailsResponse response = toRoomDetailsResponse(savedRoom);
        publishRoomUpdatedEvent(response);
        return response;
    }

    @Override
    @Transactional
    public RoomDetailsResponse joinRoom(UUID userId, RoomJoinRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BadRequestException("Room code is required");
        }

        String code = request.getCode().trim().toUpperCase();
        if (!ROOM_CODE_PATTERN.matcher(code).matches()) {
            throw new BadRequestException("Room code must consist of 6 allowed alphanumeric characters");
        }

        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Room with specified code not found"));

        syncRoomState(room);

        // Join allowed only in WAITING or FINISHED states
        if (room.getStatus() != RoomStatus.WAITING && room.getStatus() != RoomStatus.FINISHED) {
            throw new ConflictException("Cannot join room in state " + room.getStatus());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Idempotent join if already a participant
        boolean alreadyParticipant = roomParticipantRepository.existsByRoomIdAndUserId(room.getId(), userId);
        if (!alreadyParticipant) {
            RoomParticipant participant = new RoomParticipant(room, user);
            roomParticipantRepository.save(participant);

            room.setStateVersion(room.getStateVersion() + 1);
            room = roomRepository.save(room);
        }

        RoomDetailsResponse response = toRoomDetailsResponse(room);
        publishRoomUpdatedEvent(response);
        return response;
    }

    @Override
    @Transactional
    public RoomDetailsResponse getRoomDetails(UUID roomId, UUID userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        // Enforce membership privacy (return 404 for non-participants to prevent enumeration)
        if (!roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        syncRoomState(room);

        return toRoomDetailsResponse(room);
    }

    @Override
    @Transactional
    public RoomDetailsResponse startSession(UUID roomId, UUID userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (!roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        syncRoomState(room);

        // Can only start from WAITING or FINISHED
        if (room.getStatus() != RoomStatus.WAITING && room.getStatus() != RoomStatus.FINISHED) {
            throw new ConflictException("Cannot start session when room is in state " + room.getStatus());
        }

        List<RoomParticipant> participants = roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId);
        if (participants.isEmpty()) {
            throw new ConflictException("Room has no participants");
        }

        // Verify that no participant has another active IN_PROGRESS session
        for (RoomParticipant participant : participants) {
            if (sessionRepository.findByUserIdAndStatus(participant.getUser().getId(), PomodoroSessionStatus.IN_PROGRESS).isPresent()) {
                throw new ConflictException("One or more participants already have an active Pomodoro session");
            }
        }

        Instant now = Instant.now();
        room.setStatus(RoomStatus.ACTIVE);
        room.setCurrentSessionStart(now);
        room.setStateVersion(room.getStateVersion() + 1);
        Room savedRoom = roomRepository.save(room);

        // Atomically create SHARED session for every participant
        for (RoomParticipant participant : participants) {
            PomodoroSession session = new PomodoroSession(
                    participant.getUser(),
                    savedRoom,
                    PomodoroSessionType.SHARED,
                    savedRoom.getPomodoroDuration(),
                    savedRoom.getBreakDuration(),
                    now
            );
            sessionRepository.save(session);
        }

        RoomDetailsResponse response = toRoomDetailsResponse(savedRoom);
        publishRoomUpdatedEvent(response);
        return response;
    }

    @Override
    @Transactional
    public RoomDetailsResponse stopSession(UUID roomId, UUID userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (!roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        syncRoomState(room);

        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ConflictException("Cannot stop session when room is in state " + room.getStatus());
        }

        // Cancel all active sessions of this cycle without awarding focus minutes
        List<PomodoroSession> activeSessions = sessionRepository.findAllByRoomIdAndStatus(roomId, PomodoroSessionStatus.IN_PROGRESS);
        Instant now = Instant.now();
        for (PomodoroSession session : activeSessions) {
            session.setStatus(PomodoroSessionStatus.CANCELLED);
            session.setEndTime(now);
            sessionRepository.save(session);
        }

        room.setStatus(RoomStatus.FINISHED);
        room.setCurrentSessionStart(null);
        room.setStateVersion(room.getStateVersion() + 1);
        Room savedRoom = roomRepository.save(room);

        RoomDetailsResponse response = toRoomDetailsResponse(savedRoom);
        publishRoomUpdatedEvent(response);
        return response;
    }

    @Override
    @Transactional
    public MessageResponse leaveRoom(UUID roomId, UUID userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        RoomParticipant participant = roomParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        syncRoomState(room);

        // Cancel own active shared session if currently in progress
        Optional<PomodoroSession> userActiveSession = sessionRepository.findByUserIdAndStatus(userId, PomodoroSessionStatus.IN_PROGRESS);
        if (userActiveSession.isPresent() && userActiveSession.get().getRoom() != null
                && userActiveSession.get().getRoom().getId().equals(roomId)) {
            PomodoroSession session = userActiveSession.get();
            session.setStatus(PomodoroSessionStatus.CANCELLED);
            session.setEndTime(Instant.now());
            sessionRepository.save(session);
        }

        // Remove participant
        roomParticipantRepository.delete(participant);
        roomParticipantRepository.flush();

        List<RoomParticipant> remainingParticipants = roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId);
        if (remainingParticipants.isEmpty()) {
            // Delete empty room (database cascade sets foreign keys to NULL)
            roomRepository.delete(room);
        } else {
            // If the leaving user was the creator, transfer ownership by (joinedAt ASC, userId ASC)
            if (room.getCreator().getId().equals(userId)) {
                User nextCreator = remainingParticipants.getFirst().getUser();
                room.setCreator(nextCreator);
            }
            room.setStateVersion(room.getStateVersion() + 1);
            Room savedRoom = roomRepository.save(room);
            publishRoomUpdatedEvent(toRoomDetailsResponse(savedRoom));
        }

        return new MessageResponse("Left the room successfully");
    }

    @Override
    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void processExpiredRooms() {
        List<Room> activeOrBreakRooms = roomRepository.findAllByStatusIn(List.of(RoomStatus.ACTIVE, RoomStatus.BREAK));
        for (Room room : activeOrBreakRooms) {
            try {
                long initialVersion = room.getStateVersion();
                syncRoomState(room);
                if (room.getStateVersion() > initialVersion) {
                    publishRoomUpdatedEvent(toRoomDetailsResponse(room));
                }
            } catch (Exception ignored) {
                // Ignore transient scheduling errors to keep daemon running
            }
        }
    }

    private void syncRoomState(Room room) {
        Instant now = Instant.now();

        if (room.getStatus() == RoomStatus.ACTIVE) {
            Instant focusStart = room.getCurrentSessionStart();
            if (focusStart != null) {
                Instant focusEnd = focusStart.plus(room.getPomodoroDuration(), ChronoUnit.MINUTES);
                if (!now.isBefore(focusEnd)) {
                    // Mark all active sessions as completed at scheduled end time
                    List<PomodoroSession> sessions = sessionRepository.findAllByRoomIdAndStatus(room.getId(), PomodoroSessionStatus.IN_PROGRESS);
                    for (PomodoroSession session : sessions) {
                        session.setStatus(PomodoroSessionStatus.COMPLETED);
                        session.setEndTime(focusEnd);
                        sessionRepository.save(session);
                    }

                    Instant breakEnd = focusEnd.plus(room.getBreakDuration(), ChronoUnit.MINUTES);
                    if (!now.isBefore(breakEnd)) {
                        // Both focus and break expired -> transition to WAITING
                        room.setStatus(RoomStatus.WAITING);
                        room.setCurrentSessionStart(null);
                        room.setStateVersion(room.getStateVersion() + 1);
                    } else {
                        // Break is currently active
                        room.setStatus(RoomStatus.BREAK);
                        room.setCurrentSessionStart(focusEnd);
                        room.setStateVersion(room.getStateVersion() + 1);
                    }
                    roomRepository.save(room);
                }
            }
        } else if (room.getStatus() == RoomStatus.BREAK) {
            Instant breakStart = room.getCurrentSessionStart();
            if (breakStart != null) {
                Instant breakEnd = breakStart.plus(room.getBreakDuration(), ChronoUnit.MINUTES);
                if (!now.isBefore(breakEnd)) {
                    room.setStatus(RoomStatus.WAITING);
                    room.setCurrentSessionStart(null);
                    room.setStateVersion(room.getStateVersion() + 1);
                    roomRepository.save(room);
                }
            }
        }
    }

    private RoomDetailsResponse toRoomDetailsResponse(Room room) {
        List<RoomParticipant> participants = roomParticipantRepository.findAllByRoomIdWithUserOrdered(room.getId());
        List<RoomParticipantResponse> participantResponses = participants.stream()
                .map(p -> new RoomParticipantResponse(
                        p.getUser().getId(),
                        p.getUser().getUsername(),
                        p.getUser().getName(),
                        p.getUser().getId().equals(room.getCreator().getId())
                ))
                .toList();

        return new RoomDetailsResponse(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getCreator().getId(),
                room.getStatus(),
                room.getPomodoroDuration(),
                room.getBreakDuration(),
                room.getCurrentSessionStart(),
                Instant.now(),
                room.getStateVersion(),
                participantResponses
        );
    }

    private void publishRoomUpdatedEvent(RoomDetailsResponse roomDetails) {
        messagingTemplate.ifPresent(template -> {
            try {
                template.convertAndSend("/topic/rooms/" + roomDetails.getId(), RoomUpdatedEvent.of(roomDetails));
            } catch (Exception ignored) {
                // Non-blocking: fail-safe event emission
            }
        });
    }

    private String generateUniqueRoomCode() {
        for (int attempts = 0; attempts < 100; attempts++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                int index = SECURE_RANDOM.nextInt(ROOM_CODE_ALPHABET.length());
                sb.append(ROOM_CODE_ALPHABET.charAt(index));
            }
            String code = sb.toString();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique room code after 100 attempts");
    }
}
