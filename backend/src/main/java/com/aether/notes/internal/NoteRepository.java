package com.aether.notes.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Note> findByIdAndUserId(UUID id, UUID userId);
}
