package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {

    List<CardAttachment> findAllByProjectIdOrderByIdAsc(Long projectId);

    List<CardAttachment> findAllByCardIdOrderByIdAsc(Long cardId);

    long countByCardId(Long cardId);

    boolean existsByCardIdAndKindAndExternalId(Long cardId, AttachmentKind kind, String externalId);

    boolean existsByProjectIdAndKindAndExternalId(Long projectId, AttachmentKind kind, String externalId);

    /** 회원 탈퇴 시 — 첨부는 카드에 딸린 팀 작업물이라 남기고, 붙인 사람 이름만 바꾼다. */
    @Modifying
    @Query("update CardAttachment a set a.attachedByName = :name where a.attachedById = :userId")
    void renameAttacher(@Param("userId") Long userId, @Param("name") String name);
}
