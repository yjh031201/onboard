package com.kanban.backend.user.dto;

/** 회원 탈퇴 본인 확인용 — 비밀번호가 없는 소셜 로그인 계정은 password 없이 보낸다. */
public record DeleteAccountRequest(String password) {
}
