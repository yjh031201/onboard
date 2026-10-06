package com.kanban.backend.label;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelRepository extends JpaRepository<Label, String> {

    List<Label> findAllByProjectIdOrderByPositionAsc(Long projectId);

    boolean existsByIdAndProjectId(String id, Long projectId);
}
