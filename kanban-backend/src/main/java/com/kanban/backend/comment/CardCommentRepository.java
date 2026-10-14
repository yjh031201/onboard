package com.kanban.backend.comment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {

    List<CardComment> findAllByCardIdOrderByCreatedAtAscIdAsc(Long cardId);

    long countByCardId(Long cardId);

    /** 프로젝트의 카드별 댓글 수 — [cardId, count] 행들. 댓글이 없는 카드는 안 나온다. */
    @Query("select c.cardId, count(c) from CardComment c where c.projectId = :projectId group by c.cardId")
    List<Object[]> countByCardInProject(@Param("projectId") Long projectId);

    void deleteAllByCardId(Long cardId);

    void deleteAllByProjectId(Long projectId);

    /** 회원 탈퇴 시 — 댓글은 팀 대화라 남기고, 복사돼 있던 작성자 이름만 바꾼다. */
    @Modifying
    @Query("update CardComment c set c.authorName = :name where c.authorId = :userId")
    void renameAuthor(@Param("userId") Long userId, @Param("name") String name);
}
