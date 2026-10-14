package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findAllByProjectIdOrderByStatusAscPositionAsc(Long projectId);

    void deleteAllByProjectId(Long projectId);

    List<Card> findAllByProjectIdAndStatusOrderByPositionAsc(Long projectId, String status);

    boolean existsByProjectIdAndStatus(Long projectId, String status);

    @Query("select c from Card c where c.projectId = :projectId and :labelId member of c.labelIds")
    List<Card> findAllWithLabel(@Param("projectId") Long projectId, @Param("labelId") String labelId);
}
