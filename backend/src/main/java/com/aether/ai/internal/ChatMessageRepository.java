package com.aether.ai.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Newest first, so a page is the tail of the conversation. */
    @Query("""
            select m from ChatMessage m
            where m.userId = :userId
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findRecent(@Param("userId") UUID userId, Pageable pageable);

    void deleteByUserId(UUID userId);
}
