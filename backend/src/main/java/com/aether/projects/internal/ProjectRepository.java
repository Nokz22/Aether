package com.aether.projects.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
}
