package com.hub.chat.repository;

import com.hub.chat.entity.Conversation;
import com.hub.chat.entity.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "LEFT JOIN c.members m " +
           "WHERE m.userId = :userId OR c.type = 'ANNOUNCEMENT' " +
           "ORDER BY c.createdAt DESC")
    List<Conversation> findByUserIdOrAnnouncement(@Param("userId") Long userId);
}
