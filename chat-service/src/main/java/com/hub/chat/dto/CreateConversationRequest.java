package com.hub.chat.dto;

import com.hub.chat.entity.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateConversationRequest {
    @NotNull
    private ConversationType type;

    @NotBlank(message = "Le nom est requis")
    private String name;

    /** IDs des membres (pour GROUP). Pour INDIVIDUAL, un seul ID (l'autre participant). */
    private List<Long> memberUserIds;

    /** Usernames correspondants (optionnel). Si absent, "User-{id}" sera utilisé. */
    private List<String> memberUsernames;
}
