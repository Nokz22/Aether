package com.aether.projects.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProjectIdOrderByCreatedAt(UUID projectId);

    List<Task> findAllByProjectIdInOrderByCreatedAt(Collection<UUID> projectIds);

    Optional<Task> findByIdAndProjectId(UUID id, UUID projectId);
}
