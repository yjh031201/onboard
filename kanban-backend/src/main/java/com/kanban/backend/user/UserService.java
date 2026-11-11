package com.kanban.backend.user;

import com.kanban.backend.auth.dto.UserResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.user.dto.ChangePasswordRequest;
import com.kanban.backend.user.dto.UpdateProfileRequest;
import java.util.Objects;
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
        String phone = request.phone() == null || request.phone().isBlank() ? null : request.phone().trim();
        // 이메일·비밀번호로 가입한 계정은 아이디 찾기(이름+휴대폰)에 휴대폰이 필요하다. 소셜 계정은 비워 둬도 된다.
        if (phone == null && user.getPassword() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "휴대폰 번호를 입력해주세요.");
        }
        boolean phoneChanged = !Objects.equals(phone, user.getPhone());
        if (phoneChanged && phone != null && userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 휴대폰 번호입니다.");
        }

        user.changeProfile(request.name().trim(), phone);
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "구글·네이버로 가입한 계정은 비밀번호가 없어요.");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** 팀원 초대 시 이메일/휴대폰번호로 이미 가입된 사용자를 찾음. */
    @Transactional(readOnly = true)
    public UserResponse search(String keyword) {
        User found = userRepository.findByEmailOrPhone(keyword, keyword)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "일치하는 사용자를 찾을 수 없어요."));
        return UserResponse.from(found);
    }
}
