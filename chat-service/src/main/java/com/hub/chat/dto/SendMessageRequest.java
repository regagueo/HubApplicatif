package com.hub.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull
    private Long conversationId;

    @NotBlank(message = "Le contenu du message est requis")
    private String content;
}
