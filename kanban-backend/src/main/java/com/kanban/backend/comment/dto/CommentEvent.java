package com.kanban.backend.comment.dto;

import java.util.List;

/**
 * /topic/projects/{projectId}/card-comments로 나가는 실시간 이벤트 — 바뀐 카드의 댓글 전체 목록.
 * 프로젝트 하나에 토픽 하나라, 화면은 지금 열어 둔 카드(cardId)의 것만 반영한다.
 */
public record CommentEvent(
        Long cardId,
        List<CommentResponse> comments
) {
}
