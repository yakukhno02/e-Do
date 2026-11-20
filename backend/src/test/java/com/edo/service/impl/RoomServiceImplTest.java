package com.edo.service.impl;

import com.edo.dto.request.RoomCreateRequest;
import com.edo.dto.request.RoomJoinRequest;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RoomDetailsResponse;
import com.edo.entity.PomodoroSession;
import com.edo.entity.Room;
import com.edo.entity.RoomParticipant;
import com.edo.entity.User;
import com.edo.enums.PomodoroSessionStatus;
import com.edo.enums.PomodoroSessionType;
import com.edo.enums.RoomStatus;
import com.edo.enums.UserRole;
import com.edo.exception.BadRequestException;
import com.edo.exception.ConflictException;
import com.edo.exception.ResourceNotFoundException;
import com.edo.repository.PomodoroSessionRepository;
import com.edo.repository.RoomParticipantRepository;
import com.edo.repository.RoomRepository;
import com.edo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    private static final String VALID_CODE = "ABC234";

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private PomodoroSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RoomServiceImpl roomService;

    private User creator;
    private UUID creatorId;
    private User participantUser;
    private UUID participantId;
    private Room testRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        participantId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        creator = new User("creator@example.com", "creator", "Creator", "hash", UserRole.USER);
        creator.setId(creatorId);

        participantUser = new User("participant@example.com", "participant", "Participant", "hash", UserRole.USER);
        participantUser.setId(participantId);

        testRoom = new Room(VALID_CODE, "Group Study", creator, 25, 5);
        testRoom.setId(roomId);
        testRoom.setStatus(RoomStatus.WAITING);

        roomService = new RoomServiceImpl(
                roomRepository,
                roomParticipantRepository,
                sessionRepository,
                userRepository,
                Optional.of(messagingTemplate)
        );
    }

    @Test
    @DisplayName("createRoom generates code, saves room, and adds creator as first participant")
    void createRoomSuccess() {
        RoomCreateRequest request = new RoomCreateRequest();
        request.setName("Group Study");
        request.setPomodoroDuration(25);
        request.setBreakDuration(5);

        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));
        when(roomRepository.existsByCode(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            r.setId(roomId);
            return r;
        });
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId))
                .thenReturn(List.of(new RoomParticipant(testRoom, creator)));

        RoomDetailsResponse response = roomService.createRoom(creatorId, request);

        assertNotNull(response);
        assertEquals("Group Study", response.getName());
        assertEquals(creatorId, response.getCreatorId());
        verify(roomParticipantRepository).save(any(RoomParticipant.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId), any(Object.class));
    }

    @Test
    @DisplayName("joinRoom joins user when room is in WAITING status")
    void joinRoomSuccess() {
        RoomJoinRequest request = new RoomJoinRequest();
        request.setCode(VALID_CODE);

        when(roomRepository.findByCode(VALID_CODE)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(participantId)).thenReturn(Optional.of(participantUser));
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, participantId)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId))
                .thenReturn(List.of(new RoomParticipant(testRoom, creator), new RoomParticipant(testRoom, participantUser)));

        RoomDetailsResponse response = roomService.joinRoom(participantId, request);

        assertNotNull(response);
        verify(roomParticipantRepository).save(any(RoomParticipant.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId), any(Object.class));
    }

    @Test
    @DisplayName("joinRoom throws ConflictException when room is ACTIVE")
    void joinRoomActiveConflict() {
        testRoom.setStatus(RoomStatus.ACTIVE);
        testRoom.setCurrentSessionStart(Instant.now());
        RoomJoinRequest request = new RoomJoinRequest();
        request.setCode(VALID_CODE);

        when(roomRepository.findByCode(VALID_CODE)).thenReturn(Optional.of(testRoom));

        assertThrows(ConflictException.class, () -> roomService.joinRoom(participantId, request));
    }

    @Test
    @DisplayName("joinRoom throws BadRequestException when code format is invalid")
    void joinRoomInvalidCode() {
        RoomJoinRequest request = new RoomJoinRequest();
        request.setCode("invalid-code");

        assertThrows(BadRequestException.class, () -> roomService.joinRoom(participantId, request));
    }

    @Test
    @DisplayName("getRoomDetails throws ResourceNotFoundException for non-participant (privacy invariant)")
    void getRoomDetailsNonParticipantNotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, participantId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomDetails(roomId, participantId));
    }

    @Test
    @DisplayName("startSession creates shared sessions for all room participants and sets room to ACTIVE")
    void startSessionSuccess() {
        RoomParticipant rp1 = new RoomParticipant(testRoom, creator);
        RoomParticipant rp2 = new RoomParticipant(testRoom, participantUser);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, creatorId)).thenReturn(true);
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId)).thenReturn(List.of(rp1, rp2));
        when(sessionRepository.findByUserIdAndStatus(any(), eq(PomodoroSessionStatus.IN_PROGRESS))).thenReturn(Optional.empty());
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        RoomDetailsResponse response = roomService.startSession(roomId, creatorId);

        assertEquals(RoomStatus.ACTIVE, testRoom.getStatus());
        verify(sessionRepository, times(2)).save(any(PomodoroSession.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId), any(Object.class));
        assertNotNull(response);
    }

    @Test
    @DisplayName("startSession throws ConflictException if any participant already has an active session")
    void startSessionParticipantBusy() {
        RoomParticipant rp1 = new RoomParticipant(testRoom, creator);
        PomodoroSession busySession = new PomodoroSession(creator, null, PomodoroSessionType.INDIVIDUAL, 25, 5, Instant.now());

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomParticipantRepository.existsByRoomIdAndUserId(roomId, creatorId)).thenReturn(true);
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId)).thenReturn(List.of(rp1));
        when(sessionRepository.findByUserIdAndStatus(creatorId, PomodoroSessionStatus.IN_PROGRESS)).thenReturn(Optional.of(busySession));

        assertThrows(ConflictException.class, () -> roomService.startSession(roomId, creatorId));
    }

    @Test
    @DisplayName("leaveRoom deletes empty room when last participant leaves")
    void leaveRoomDeletesWhenEmpty() {
        RoomParticipant rp = new RoomParticipant(testRoom, creator);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomParticipantRepository.findByRoomIdAndUserId(roomId, creatorId)).thenReturn(Optional.of(rp));
        when(sessionRepository.findByUserIdAndStatus(creatorId, PomodoroSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId)).thenReturn(List.of());

        MessageResponse response = roomService.leaveRoom(roomId, creatorId);

        verify(roomParticipantRepository).delete(rp);
        verify(roomRepository).delete(testRoom);
        assertEquals("Left the room successfully", response.getMessage());
    }

    @Test
    @DisplayName("leaveRoom transfers creator ownership to next participant when creator leaves non-empty room")
    void leaveRoomTransfersOwnership() {
        RoomParticipant rpCreator = new RoomParticipant(testRoom, creator);
        RoomParticipant rpNext = new RoomParticipant(testRoom, participantUser);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(roomParticipantRepository.findByRoomIdAndUserId(roomId, creatorId)).thenReturn(Optional.of(rpCreator));
        when(sessionRepository.findByUserIdAndStatus(creatorId, PomodoroSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(roomParticipantRepository.findAllByRoomIdWithUserOrdered(roomId)).thenReturn(List.of(rpNext));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        roomService.leaveRoom(roomId, creatorId);

        assertEquals(participantUser, testRoom.getCreator());
        verify(roomRepository).save(testRoom);
        verify(messagingTemplate).convertAndSend(eq("/topic/rooms/" + roomId), any(Object.class));
    }
}
