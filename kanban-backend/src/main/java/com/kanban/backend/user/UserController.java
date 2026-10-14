package com.kanban.backend.user;

import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.user.dto.ChangePasswordRequest;
import com.kanban.backend.user.dto.DeleteAccountRequest;
import com.kanban.backend.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 필요 — SecurityConfig에서 /api/auth/** 외에는 전부 인증 필요하도록 되어 있음. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;

    public UserController(UserService userService, AccountDeletionService accountDeletionService) {
        this.userService = userService;
        this.accountDeletionService = accountDeletionService;
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(user, request));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    /** 회원 탈퇴 — 본인 확인용 비밀번호를 body로 받는다 (소셜 로그인 전용 계정은 body 없이도 됨). */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @AuthenticationPrincipal User user,
            @RequestBody(required = false) DeleteAccountRequest request
    ) {
        accountDeletionService.delete(user, request);
        return ResponseEntity.noContent().build();
    }

    /** 멤버 초대용 후보 검색 — 가입된 전체 사용자 목록(프로젝트 소속 여부와 무관). */
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.listAll());
    }

    /** 팀원 초대용 — 이메일 또는 휴대폰번호로 이미 가입된 사용자 검색. */
    @GetMapping("/search")
    public ResponseEntity<UserResponse> search(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.search(keyword));
    }
}
