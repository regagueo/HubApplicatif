package com.hub.chat.controller;

import com.hub.chat.dto.MessageDto;
import com.hub.chat.dto.SendMessageRequest;
import com.hub.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/send")
    public MessageDto sendMessage(@Payload SendMessageRequest request) {
        return chatService.sendMessage(request);
    }
}
