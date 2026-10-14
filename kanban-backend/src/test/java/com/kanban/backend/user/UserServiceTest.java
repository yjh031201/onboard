package com.kanban.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kanban.backend.common.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

// 팀원 권한 변경(changeRole)은 프로젝트별 권한으로 바뀌면서 ProjectMemberService로 옮겨갔다
// (project 패키지의 ProjectMemberServiceTest 참고 — 아직 없다면 거기에 추가할 것).
// 여기 남은 건 여전히 전역인 listAll/search만.
class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    private User userWithId(long id, String email, String phone, UserRole role) {
        User user = new User(email, "encoded", "이름" + id, phone);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }

    @Test
    void listAll_returnsEveryRegisteredUser() {
        when(userRepository.findAll()).thenReturn(List.of(
                userWithId(1L, "a@example.com", "010-1111-1111", UserRole.OWNER),
                userWithId(2L, "b@example.com", "010-2222-2222", UserRole.MEMBER)
        ));

        List<?> result = userService.listAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void search_findsUserByEmailOrPhoneKeyword() {
        User target = userWithId(2L, "b@example.com", "010-2222-2222", UserRole.MEMBER);
        when(userRepository.findByEmailOrPhone("b@example.com", "b@example.com")).thenReturn(Optional.of(target));

        var response = userService.search("b@example.com");

        assertThat(response.id()).isEqualTo(2L);
    }

    @Test
    void search_rejectsWhenNoMatch() {
        when(userRepository.findByEmailOrPhone(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.search("nobody@example.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("일치하는 사용자");
    }
}
