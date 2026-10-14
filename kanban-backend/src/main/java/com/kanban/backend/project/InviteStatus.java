package com.kanban.backend.project;

/**
 * ProjectMember 한 행의 초대 상태. 초대를 보내면 PENDING으로 생기고, 초대받은 사람이
 * 수락해야 ACCEPTED로 바뀌어 실제 멤버(보드/라벨/타임라인 접근 등)로 취급된다.
 * 거절하면 그 행 자체를 지운다 — 다시 초대받으려면 관리자가 새로 초대해야 한다.
 */
public enum InviteStatus {
    PENDING,
    ACCEPTED
}
