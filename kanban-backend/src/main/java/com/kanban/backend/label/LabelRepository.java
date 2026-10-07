package com.kanban.backend.label;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, String> {

    List<Label> findAllByProjectIdOrderByPositionAsc(Long projectId);

    void deleteAllByProjectId(Long projectId);

    boolean existsByIdAndProjectId(String id, Long projectId);

    Optional<Label> findByProjectIdAndIsEtcTrue(Long projectId);
}
