package com.kanban.backend.user;

import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.user.dto.ChangePasswordRequest;
import com.kanban.backend.user.dto.UpdateProfileRequest;
import com.kanban.backend.user.dto.UpdateRoleRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    public UserController(UserService userService) {
        this.userService = userService;
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

    /** 팀원 페이지 목록 — 단일 팀 구조라 가입된 전체 사용자를 반환. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userService.listAll());
    }

    /** 팀원 초대용 — 이메일 또는 휴대폰번호로 이미 가입된 사용자 검색. */
    @GetMapping("/search")
    public ResponseEntity<UserResponse> search(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.search(keyword));
    }

    /** 팀원 권한 변경 — OWNER/ADMIN만 호출 가능 (UserService에서 검증). */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal User requester,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(userService.changeRole(requester, id, request.role()));
    }
}
