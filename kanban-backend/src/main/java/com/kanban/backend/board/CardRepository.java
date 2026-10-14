package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findAllByProjectIdOrderByStatusAscPositionAsc(Long projectId);

    void deleteAllByProjectId(Long projectId);

    List<Card> findAllByProjectIdAndStatusOrderByPositionAsc(Long projectId, String status);

    boolean existsByProjectIdAndStatus(Long projectId, String status);

    @Query("select c from Card c where c.projectId = :projectId and :labelId member of c.labelIds")
    List<Card> findAllWithLabel(@Param("projectId") Long projectId, @Param("labelId") String labelId);

    /** 회원 탈퇴 시 — 카드는 팀 작업물이라 남기고, 카드에 복사돼 있던 작성자 이름만 바꾼다. */
    @Modifying
    @Query("update Card c set c.createdByName = :name where c.createdById = :userId")
    void renameCreator(@Param("userId") Long userId, @Param("name") String name);
}
