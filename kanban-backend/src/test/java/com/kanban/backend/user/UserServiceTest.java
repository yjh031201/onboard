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

    @Test
    void changeRole_adminCanPromoteMemberToAdmin() {
        User admin = userWithId(1L, "admin@example.com", "010-1111-1111", UserRole.ADMIN);
        User target = userWithId(2L, "member@example.com", "010-2222-2222", UserRole.MEMBER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.changeRole(admin, 2L, UserRole.ADMIN);

        assertThat(response.role()).isEqualTo("ADMIN");
    }

    @Test
    void changeRole_memberCannotChangeAnyonesRole() {
        User member = userWithId(1L, "member@example.com", "010-1111-1111", UserRole.MEMBER);

        assertThatThrownBy(() -> userService.changeRole(member, 2L, UserRole.ADMIN))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("권한이 없어요");
    }

    @Test
    void changeRole_rejectsChangingOwnRole() {
        User admin = userWithId(1L, "admin@example.com", "010-1111-1111", UserRole.ADMIN);

        assertThatThrownBy(() -> userService.changeRole(admin, 1L, UserRole.MEMBER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("본인의 권한");
    }

    @Test
    void changeRole_adminCannotGrantOwnerRole() {
        User admin = userWithId(1L, "admin@example.com", "010-1111-1111", UserRole.ADMIN);
        User target = userWithId(2L, "member@example.com", "010-2222-2222", UserRole.MEMBER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> userService.changeRole(admin, 2L, UserRole.OWNER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("소유자만");
    }

    @Test
    void changeRole_adminCannotDemoteAnOwner() {
        User admin = userWithId(1L, "admin@example.com", "010-1111-1111", UserRole.ADMIN);
        User owner = userWithId(2L, "owner@example.com", "010-2222-2222", UserRole.OWNER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> userService.changeRole(admin, 2L, UserRole.MEMBER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("소유자만");
    }

    @Test
    void changeRole_ownerCanPromoteAdminToOwner() {
        User owner = userWithId(1L, "owner@example.com", "010-1111-1111", UserRole.OWNER);
        User target = userWithId(2L, "admin@example.com", "010-2222-2222", UserRole.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.changeRole(owner, 2L, UserRole.OWNER);

        assertThat(response.role()).isEqualTo("OWNER");
    }
}
