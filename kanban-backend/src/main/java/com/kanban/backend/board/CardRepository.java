package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findAllByOrderByStatusAscPositionAsc();

    List<Card> findAllByStatusOrderByPositionAsc(String status);

    boolean existsByStatus(String status);

    @Query("select c from Card c where :labelId member of c.labelIds")
    List<Card> findAllWithLabel(@Param("labelId") String labelId);
}
