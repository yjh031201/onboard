package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, String> {

    List<BoardColumn> findAllByProjectIdOrderByPositionAsc(Long projectId);

    void deleteAllByProjectId(Long projectId);

    long countByProjectId(Long projectId);
}
