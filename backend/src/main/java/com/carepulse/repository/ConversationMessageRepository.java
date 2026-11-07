package com.carepulse.repository;

import com.carepulse.entity.Conversation;
import com.carepulse.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationOrderBySentAtAsc(Conversation conversation);

    long countByConversationAndSenderRoleNotAndReadFalse(Conversation conversation, String senderRole);

    @Modifying
    @Query("UPDATE ConversationMessage m SET m.read = true WHERE m.conversation = :conversation AND m.senderRole != :readerRole AND m.read = false")
    int markIncomingMessagesAsRead(@Param("conversation") Conversation conversation, @Param("readerRole") String readerRole);
}

