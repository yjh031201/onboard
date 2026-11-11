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

    /**
     * 이 프로젝트의 카드 순서를 바꾸는 작업(추가·이동·삭제)을 한 번에 하나씩만 하도록 프로젝트 행을 잠근다.
     * 트랜잭션이 끝날 때 풀린다. 동시에 카드를 옮기면 position이 겹치거나 비던 문제를 막는다.
     * 결과가 없으면(없는 프로젝트) null.
     */
    @Query(value = "SELECT id FROM projects WHERE id = :projectId FOR UPDATE", nativeQuery = true)
    Long lockBoard(@Param("projectId") Long projectId);

    @Query("select c from Card c where c.projectId = :projectId and :labelId member of c.labelIds")
    List<Card> findAllWithLabel(@Param("projectId") Long projectId, @Param("labelId") String labelId);

    /** 회원 탈퇴 시 — 카드는 팀 작업물이라 남기고, 카드에 복사돼 있던 작성자 이름만 바꾼다. */
    @Modifying
    @Query("update Card c set c.createdByName = :name where c.createdById = :userId")
    void renameCreator(@Param("userId") Long userId, @Param("name") String name);

    /** 회원 탈퇴 시 — 담당자로 지정돼 있던 카드도 남기고, 담당자 이름만 바꾼다. */
    @Modifying
    @Query("update Card c set c.assigneeName = :name where c.assigneeId = :userId")
    void renameAssignee(@Param("userId") Long userId, @Param("name") String name);
}
