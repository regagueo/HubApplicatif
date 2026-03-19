package com.hub.chat.config;

import com.hub.chat.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import com.hub.chat.security.ChatUserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtValidator jwtValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = getTokenFromHeaders(accessor);
            if (StringUtils.hasText(token) && jwtValidator.validateToken(token)) {
                Long userId = jwtValidator.getUserId(token);
                String username = jwtValidator.getUsername(token);
                var principal = new ChatUserPrincipal(userId, username, Collections.emptyList());
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                accessor.setUser(auth);
            }
        }
        if (accessor.getUser() instanceof org.springframework.security.core.Authentication auth) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        return message;
    }

    private String getTokenFromHeaders(StompHeaderAccessor accessor) {
        List<String> authHeader = accessor.getNativeHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String bearer = authHeader.get(0);
            if (bearer != null && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        List<String> tokenHeader = accessor.getNativeHeader("X-Auth-Token");
        if (tokenHeader != null && !tokenHeader.isEmpty() && StringUtils.hasText(tokenHeader.get(0))) {
            return tokenHeader.get(0);
        }
        return null;
    }
}
