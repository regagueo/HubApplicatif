package com.hub.chat.controller;

import com.hub.chat.dto.*;
import com.hub.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public List<ConversationDto> getConversations() {
        return chatService.getMyConversations();
    }

    @GetMapping("/conversations/{id}")
    public ConversationDto getConversation(@PathVariable Long id) {
        return chatService.getConversation(id);
    }

    @PostMapping("/conversations")
    public ConversationDto createConversation(@Valid @RequestBody CreateConversationRequest request) {
        return chatService.createConversation(request);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageDto> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return chatService.getMessages(conversationId, page, size);
    }

    @PostMapping("/messages")
    public MessageDto sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(request);
    }
}
