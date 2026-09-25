package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findAllByOrderByStatusAscPositionAsc();

    List<Card> findAllByStatusOrderByPositionAsc(String status);

    boolean existsByStatus(String status);

    List<Card> findAllByLabelId(String labelId);
}
