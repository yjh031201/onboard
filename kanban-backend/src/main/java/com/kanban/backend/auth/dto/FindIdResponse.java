package com.kanban.backend.auth.dto;

/** maskedEmail 예: jd***@gmail.com — 실제 이메일 전체를 그대로 노출하지 않음. */
public record FindIdResponse(String maskedEmail) {
}
