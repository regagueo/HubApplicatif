package com.hub.chat.config;

import com.hub.chat.entity.Conversation;
import com.hub.chat.entity.ConversationMember;
import com.hub.chat.entity.ConversationType;
import com.hub.chat.repository.ConversationMemberRepository;
import com.hub.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    @Override
    public void run(String... args) {
        boolean hasAnnouncement = conversationRepository.findAll().stream()
                .anyMatch(c -> c.getType() == ConversationType.ANNOUNCEMENT);
        if (!hasAnnouncement) {
            Conversation annonces = Conversation.builder()
                    .type(ConversationType.ANNOUNCEMENT)
                    .name("Annonces")
                    .createdBy(1L)
                    .build();
            annonces = conversationRepository.save(annonces);
            ConversationMember admin = ConversationMember.builder()
                    .conversation(annonces)
                    .userId(1L)
                    .username("admin")
                    .admin(true)
                    .build();
            memberRepository.save(admin);
        }
    }
}
