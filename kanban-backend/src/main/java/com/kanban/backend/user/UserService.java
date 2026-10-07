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

    /** 멤버 초대 시 검색 후보로 전체 가입자 목록을 보여주기 위함 — 프로젝트 소속 여부와 무관. */
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
}
