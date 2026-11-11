package com.kanban.backend.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.dto.InviteMemberRequest;
import com.kanban.backend.project.dto.UpdateMemberRoleRequest;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import com.kanban.backend.user.UserRole;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** 소유자는 프로젝트마다 한 명 — 초대로 만들 수 없고, 넘기기만 되며, 소유자끼리 강등하거나 스스로 강등할 수 없다. */
class ProjectMemberServiceRoleTest {

    private static final Long PROJECT_ID = 1L;

    private ProjectMemberRepository projectMemberRepository;
    private ProjectAccessService projectAccessService;
    private UserRepository userRepository;
    private ProjectMemberService service;

    private final User owner = user(1L);
    private final User admin = user(2L);
    private final User member = user(3L);

    @BeforeEach
    void setUp() {
        projectMemberRepository = mock(ProjectMemberRepository.class);
        projectAccessService = mock(ProjectAccessService.class);
        userRepository = mock(UserRepository.class);
        service = new ProjectMemberService(projectMemberRepository, mock(ProjectRepository.class), projectAccessService, userRepository);
        for (User u : new User[] {owner, admin, member}) {
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
    }

    private static User user(Long id) {
        User user = new User("u" + id + "@example.com", "encoded", "사용자" + id, "010-0000-000" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ProjectMember accepted(User user, UserRole role) {
        ProjectMember membership = new ProjectMember(PROJECT_ID, user.getId(), role);
        ReflectionTestUtils.setField(membership, "joinedAt", LocalDateTime.now()); // 저장할 때 채워지는 값
        when(projectMemberRepository.findByProjectIdAndUserIdAndStatus(PROJECT_ID, user.getId(), InviteStatus.ACCEPTED))
                .thenReturn(Optional.of(membership));
        return membership;
    }

    @Test
    void 소유자로는_초대할_수_없다() {
        assertThatThrownBy(() -> service.invite(PROJECT_ID, new InviteMemberRequest(member.getId(), UserRole.OWNER), admin))
                .isInstanceOf(ApiException.class);
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void 관리자나_멤버로는_초대할_수_있다() {
        when(projectMemberRepository.findByProjectIdAndUserId(PROJECT_ID, member.getId())).thenReturn(Optional.empty());
        when(projectMemberRepository.save(any())).thenAnswer(inv -> {
            ProjectMember saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "joinedAt", LocalDateTime.now());
            return saved;
        });

        assertThat(service.invite(PROJECT_ID, new InviteMemberRequest(member.getId(), UserRole.ADMIN), admin).role())
                .isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 소유자를_넘기면_나는_관리자가_된다() {
        ProjectMember ownerMembership = accepted(owner, UserRole.OWNER);
        when(projectAccessService.requireOwner(PROJECT_ID, owner)).thenReturn(ownerMembership);
        ProjectMember target = accepted(member, UserRole.MEMBER);

        service.changeRole(PROJECT_ID, member.getId(), new UpdateMemberRoleRequest(UserRole.OWNER), owner);

        assertThat(target.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(ownerMembership.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 일반_권한_변경은_소유자를_그대로_둔다() {
        ProjectMember ownerMembership = accepted(owner, UserRole.OWNER);
        when(projectAccessService.requireOwner(PROJECT_ID, owner)).thenReturn(ownerMembership);
        ProjectMember target = accepted(member, UserRole.MEMBER);

        service.changeRole(PROJECT_ID, member.getId(), new UpdateMemberRoleRequest(UserRole.ADMIN), owner);

        assertThat(target.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(ownerMembership.getRole()).isEqualTo(UserRole.OWNER);
    }

    @Test
    void 내_권한은_바꿀_수_없다() {
        ProjectMember ownerMembership = accepted(owner, UserRole.OWNER);
        when(projectAccessService.requireOwner(PROJECT_ID, owner)).thenReturn(ownerMembership);

        assertThatThrownBy(() -> service.changeRole(PROJECT_ID, owner.getId(), new UpdateMemberRoleRequest(UserRole.MEMBER), owner))
                .isInstanceOf(ApiException.class);
        assertThat(ownerMembership.getRole()).isEqualTo(UserRole.OWNER);
    }

    @Test
    void 다른_소유자는_강등할_수_없다() {
        ProjectMember ownerMembership = accepted(owner, UserRole.OWNER);
        when(projectAccessService.requireOwner(PROJECT_ID, owner)).thenReturn(ownerMembership);
        ProjectMember otherOwner = accepted(admin, UserRole.OWNER);

        assertThatThrownBy(() -> service.changeRole(PROJECT_ID, admin.getId(), new UpdateMemberRoleRequest(UserRole.MEMBER), owner))
                .isInstanceOf(ApiException.class);
        assertThat(otherOwner.getRole()).isEqualTo(UserRole.OWNER);
    }
}
