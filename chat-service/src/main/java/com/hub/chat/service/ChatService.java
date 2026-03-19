package com.hub.chat.service;

import com.hub.chat.dto.*;
import com.hub.chat.entity.*;
import com.hub.chat.repository.ConversationMemberRepository;
import com.hub.chat.repository.ConversationRepository;
import com.hub.chat.repository.MessageRepository;
import com.hub.chat.security.ChatUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatUserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ChatUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return (ChatUserPrincipal) auth.getPrincipal();
    }

    private boolean isAdmin() {
        return getCurrentUser().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getMyConversations() {
        ChatUserPrincipal user = getCurrentUser();
        List<Conversation> conversations = conversationRepository.findByUserIdOrAnnouncement(user.getUserId());
        List<ConversationDto> result = new ArrayList<>();
        for (Conversation c : conversations) {
            List<ConversationMember> members = memberRepository.findByConversationId(c.getId());
            result.add(toConversationDto(c, members, user.getUserId()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversation(Long id) {
        ChatUserPrincipal user = getCurrentUser();
        Conversation c = conversationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable"));
        if (!canAccess(c, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        List<ConversationMember> members = memberRepository.findByConversationId(c.getId());
        return toConversationDto(c, members, user.getUserId());
    }

    @Transactional
    public ConversationDto createConversation(CreateConversationRequest req) {
        ChatUserPrincipal user = getCurrentUser();

        if (req.getType() == ConversationType.ANNOUNCEMENT && !isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les administrateurs peuvent créer un canal d'annonces");
        }

        List<Long> memberIds = req.getMemberUserIds() != null ? req.getMemberUserIds() : new ArrayList<>();
        List<String> memberNames = req.getMemberUsernames() != null ? req.getMemberUsernames() : new ArrayList<>();

        if (req.getType() == ConversationType.INDIVIDUAL && memberIds.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une conversation privée requiert exactement un autre participant");
        }
        if (req.getType() == ConversationType.GROUP && memberIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un groupe requiert au moins un membre");
        }

        Conversation conv = Conversation.builder()
                .type(req.getType())
                .name(req.getName())
                .createdBy(user.getUserId())
                .build();
        conv = conversationRepository.save(conv);

        if (req.getType() == ConversationType.ANNOUNCEMENT) {
            ConversationMember admin = ConversationMember.builder()
                    .conversation(conv)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .admin(true)
                    .build();
            memberRepository.save(admin);
        } else {
            ConversationMember creator = ConversationMember.builder()
                    .conversation(conv)
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .admin(true)
                    .build();
            memberRepository.save(creator);

            for (int i = 0; i < memberIds.size(); i++) {
                Long mid = memberIds.get(i);
                if (mid.equals(user.getUserId())) continue;
                String uname = i < memberNames.size() ? memberNames.get(i) : "User-" + mid;
                ConversationMember m = ConversationMember.builder()
                        .conversation(conv)
                        .userId(mid)
                        .username(uname)
                        .admin(false)
                        .build();
                memberRepository.save(m);
            }
        }

        List<ConversationMember> members = memberRepository.findByConversationId(conv.getId());
        return toConversationDto(conv, members, user.getUserId());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(Long conversationId, int page, int size) {
        ChatUserPrincipal user = getCurrentUser();
        Conversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable"));
        if (!canAccess(c, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size));
        return messages.stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDto sendMessage(SendMessageRequest req) {
        ChatUserPrincipal user = getCurrentUser();
        Conversation c = conversationRepository.findById(req.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation introuvable"));

        if (!canAccess(c, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }

        if (c.getType() == ConversationType.ANNOUNCEMENT) {
            Optional<ConversationMember> member = memberRepository.findByConversationIdAndUserId(c.getId(), user.getUserId());
            if (member.isEmpty() || !member.get().isAdmin()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les administrateurs peuvent publier des annonces");
            }
        }

        Message msg = Message.builder()
                .conversation(c)
                .senderId(user.getUserId())
                .senderUsername(user.getUsername())
                .content(req.getContent())
                .build();
        msg = messageRepository.save(msg);

        MessageDto dto = toMessageDto(msg);

        String dest = "/topic/conversations/" + c.getId();
        messagingTemplate.convertAndSend(dest, dto);

        return dto;
    }

    private boolean canAccess(Conversation c, Long userId) {
        if (c.getType() == ConversationType.ANNOUNCEMENT) return true;
        return memberRepository.existsByConversationIdAndUserId(c.getId(), userId);
    }

    private ConversationDto toConversationDto(Conversation c, List<ConversationMember> members, Long currentUserId) {
        List<ConversationDto.MemberDto> memberDtos = members.stream()
                .map(m -> ConversationDto.MemberDto.builder()
                        .userId(m.getUserId())
                        .username(m.getUsername())
                        .admin(m.isAdmin())
                        .build())
                .collect(Collectors.toList());

        return ConversationDto.builder()
                .id(c.getId())
                .type(c.getType())
                .name(c.getName())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .members(memberDtos)
                .unreadCount(0)
                .build();
    }

    private MessageDto toMessageDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
