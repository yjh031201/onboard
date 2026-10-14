package com.kanban.backend.integration;

/** OAuth state로 되찾는 "누가, 어느 프로젝트에 연동을 시작했는지". */
public record ConnectState(Long userId, Long projectId) {
}
