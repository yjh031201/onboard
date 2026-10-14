package com.kanban.backend.board;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardAttachmentRepository extends JpaRepository<CardAttachment, Long> {

    List<CardAttachment> findAllByProjectIdOrderByIdAsc(Long projectId);

    List<CardAttachment> findAllByCardIdOrderByIdAsc(Long cardId);

    long countByCardId(Long cardId);

    boolean existsByCardIdAndKindAndExternalId(Long cardId, AttachmentKind kind, String externalId);

    boolean existsByProjectIdAndKindAndExternalId(Long projectId, AttachmentKind kind, String externalId);
}
