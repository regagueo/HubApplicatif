package com.hub.chat.dto;

import com.hub.chat.entity.ConversationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private Long id;
    private ConversationType type;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<MemberDto> members;
    private Integer unreadCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private Long userId;
        private String username;
        private boolean admin;
    }
}
