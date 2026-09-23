package com.kanban.backend.user;

import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.user.dto.ChangePasswordRequest;
import com.kanban.backend.user.dto.UpdateProfileRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인한 본인의 프로필(이름/휴대폰)과 비밀번호를 관리 — 회원가입/로그인 자체는 AuthService 담당. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        boolean phoneChanged = !request.phone().equals(user.getPhone());
        if (phoneChanged && userRepository.existsByPhone(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 휴대폰 번호입니다.");
        }

        user.changeProfile(request.name(), request.phone());
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** 팀원 페이지 목록용 — 이 워크스페이스는 단일 팀 구조라 가입된 전체 사용자가 곧 팀원임. */
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** 팀원 초대 시 이메일/휴대폰번호로 이미 가입된 사용자를 찾음. */
    @Transactional(readOnly = true)
    public UserResponse search(String keyword) {
        User found = userRepository.findByEmailOrPhone(keyword, keyword)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일치하는 사용자를 찾을 수 없어요."));
        return UserResponse.from(found);
    }

    /**
     * 팀원 권한 변경 — OWNER/ADMIN만 호출 가능.
     * OWNER 권한을 주거나 빼앗는 변경은 OWNER만 할 수 있음 (ADMIN이 서로를 소유자로 올리는 것 방지).
     */
    @Transactional
    public UserResponse changeRole(User requester, Long targetUserId, UserRole newRole) {
        if (requester.getRole() == UserRole.MEMBER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "권한을 변경할 권한이 없어요.");
        }
        if (requester.getId().equals(targetUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "본인의 권한은 변경할 수 없어요.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없어요."));

        boolean touchesOwnerRole = newRole == UserRole.OWNER || target.getRole() == UserRole.OWNER;
        if (touchesOwnerRole && requester.getRole() != UserRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "소유자 권한은 소유자만 변경할 수 있어요.");
        }

        target.changeRole(newRole);
        User saved = userRepository.save(target);
        return UserResponse.from(saved);
    }
}
