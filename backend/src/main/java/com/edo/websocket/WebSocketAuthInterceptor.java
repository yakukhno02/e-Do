package com.edo.websocket;

import com.edo.repository.RoomParticipantRepository;
import com.edo.security.CustomUserDetailsService;
import com.edo.security.JwtTokenProvider;
import com.edo.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/rooms/([a-fA-F0-9\\-]+)$");

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RoomParticipantRepository roomParticipantRepository;

    public WebSocketAuthInterceptor(JwtTokenProvider tokenProvider,
                                    CustomUserDetailsService userDetailsService,
                                    RoomParticipantRepository roomParticipantRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.roomParticipantRepository = roomParticipantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(authHeader)) {
            authHeader = accessor.getFirstNativeHeader("token");
        }

        String jwt = null;
        if (StringUtils.hasText(authHeader)) {
            if (authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            } else {
                jwt = authHeader;
            }
        }

        if (!StringUtils.hasText(jwt) || !tokenProvider.validateToken(jwt)) {
            log.warn("STOMP CONNECT rejected: missing or invalid JWT");
            throw new AccessDeniedException("Full authentication is required to access this resource");
        }

        try {
            UUID userId = tokenProvider.getUserIdFromToken(jwt);
            int tokenCredentialsVersion = tokenProvider.getCredentialsVersionFromToken(jwt);

            UserPrincipal userDetails = userDetailsService.loadUserById(userId);
            if (userDetails.getCredentialsVersion() != tokenCredentialsVersion) {
                log.warn("STOMP CONNECT rejected: outdated credentialsVersion for user {}", userId);
                throw new AccessDeniedException("Full authentication is required to access this resource");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
            log.info("STOMP CONNECT authenticated user {}", userId);
        } catch (Exception e) {
            log.warn("STOMP CONNECT authentication failed: {}", e.getMessage());
            throw new AccessDeniedException("Full authentication is required to access this resource");
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = ROOM_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            Principal principal = accessor.getUser();
            if (!(principal instanceof UsernamePasswordAuthenticationToken authToken) ||
                    !(authToken.getPrincipal() instanceof UserPrincipal userPrincipal)) {
                log.warn("STOMP SUBSCRIBE rejected: unauthenticated user trying to subscribe to {}", destination);
                throw new AccessDeniedException("Full authentication is required to access this resource");
            }

            UUID roomId;
            try {
                roomId = UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException e) {
                log.warn("STOMP SUBSCRIBE rejected: invalid UUID in destination {}", destination);
                throw new AccessDeniedException("Access is denied");
            }

            UUID userId = userPrincipal.getId();
            boolean isParticipant = roomParticipantRepository.existsByRoomIdAndUserId(roomId, userId);
            if (!isParticipant) {
                log.warn("STOMP SUBSCRIBE rejected: user {} is not participant of room {}", userId, roomId);
                throw new AccessDeniedException("Access is denied");
            }

            log.info("STOMP SUBSCRIBE accepted: user {} subscribed to room {}", userId, roomId);
        }
    }
}
