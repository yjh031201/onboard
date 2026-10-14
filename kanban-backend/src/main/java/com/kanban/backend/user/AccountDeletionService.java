package com.kanban.backend.user;

import com.kanban.backend.auth.RefreshTokenStore;
import com.kanban.backend.auth.oauth2.OAuthExchangeCodeRepository;
import com.kanban.backend.board.CardAttachmentRepository;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.comment.CardCommentRepository;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.FileStorageService;
import com.kanban.backend.file.ProjectFile;
import com.kanban.backend.file.ProjectFileRepository;
import com.kanban.backend.integration.IntegrationRepository;
import com.kanban.backend.project.InviteStatus;
import com.kanban.backend.project.Project;
import com.kanban.backend.project.ProjectMember;
import com.kanban.backend.project.ProjectMemberRepository;
import com.kanban.backend.project.ProjectRepository;
import com.kanban.backend.project.ProjectService;
import com.kanban.backend.schedule.ScheduleRepository;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.user.dto.DeleteAccountRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 — 계정과 그 사람의 개인정보를 지운다.
 *
 * <ul>
 *   <li>혼자 쓰던(다른 수락 멤버가 없는) 프로젝트는 프로젝트째 삭제</li>
 *   <li>다른 멤버가 있는 프로젝트의 유일한 OWNER면 탈퇴를 막는다 — 관리할 사람이 없는 프로젝트가 생기므로</li>
 *   <li>본인이 올린 파일·만든 일정·연결한 연동은 삭제</li>
 *   <li>카드·카드 첨부·타임라인 기록은 팀 작업물이라 남기고, 이름만 {@link #WITHDRAWN_USER_NAME}로 바꾼다</li>
 * </ul>
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    /** 탈퇴 후에도 남는 카드·타임라인 기록에 이름 대신 들어가는 표시. */
    static final String WITHDRAWN_USER_NAME = "탈퇴한 사용자";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final ProjectFileRepository projectFileRepository;
    private final FileStorageService fileStorageService;
    private final ScheduleRepository scheduleRepository;
    private final IntegrationRepository integrationRepository;
    private final OAuthExchangeCodeRepository oauthExchangeCodeRepository;
    private final CardRepository cardRepository;
    private final CardAttachmentRepository cardAttachmentRepository;
    private final CardCommentRepository commentRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final RefreshTokenStore refreshTokenStore;

    public AccountDeletionService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectService projectService,
            ProjectFileRepository projectFileRepository,
            FileStorageService fileStorageService,
            ScheduleRepository scheduleRepository,
            IntegrationRepository integrationRepository,
            OAuthExchangeCodeRepository oauthExchangeCodeRepository,
            CardRepository cardRepository,
            CardAttachmentRepository cardAttachmentRepository,
            CardCommentRepository commentRepository,
            TimelineEventRepository timelineEventRepository,
            RefreshTokenStore refreshTokenStore
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.projectFileRepository = projectFileRepository;
        this.fileStorageService = fileStorageService;
        this.scheduleRepository = scheduleRepository;
        this.integrationRepository = integrationRepository;
        this.oauthExchangeCodeRepository = oauthExchangeCodeRepository;
        this.cardRepository = cardRepository;
        this.cardAttachmentRepository = cardAttachmentRepository;
        this.commentRepository = commentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public void delete(User user, DeleteAccountRequest request) {
        verifyPassword(user, request);
        Long userId = user.getId();

        // 1) 프로젝트 — 지울 것과 탈퇴를 막을 것을 먼저 가려낸다 (하나라도 막히면 아무것도 지우지 않는다).
        List<Long> soloProjectIds = new ArrayList<>();
        List<String> blockingProjectNames = new ArrayList<>();
        for (ProjectMember membership : projectMemberRepository.findAllByUserId(userId)) {
            if (membership.getStatus() != InviteStatus.ACCEPTED || membership.getRole() != UserRole.OWNER) {
                continue;
            }
            List<ProjectMember> others = projectMemberRepository
                    .findAllByProjectIdAndStatus(membership.getProjectId(), InviteStatus.ACCEPTED).stream()
                    .filter(other -> !other.getUserId().equals(userId))
                    .toList();
            if (others.isEmpty()) {
                soloProjectIds.add(membership.getProjectId());
            } else if (others.stream().noneMatch(other -> other.getRole() == UserRole.OWNER)) {
                blockingProjectNames.add(projectRepository.findById(membership.getProjectId())
                        .map(Project::getName)
                        .orElse("이름 없는 프로젝트"));
            }
        }
        if (!blockingProjectNames.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "다른 팀원이 있는 프로젝트의 소유자예요: " + String.join(", ", blockingProjectNames)
                            + ". 다른 팀원을 소유자로 지정하거나 프로젝트를 삭제한 뒤 탈퇴해주세요.");
        }

        soloProjectIds.forEach(projectId -> projectService.delete(projectId, user));
        projectMemberRepository.deleteAllByUserId(userId);

        // 2) 팀에 남는 기록은 이름만 지운다.
        cardRepository.renameCreator(userId, WITHDRAWN_USER_NAME);
        cardRepository.renameAssignee(userId, WITHDRAWN_USER_NAME);
        cardAttachmentRepository.renameAttacher(userId, WITHDRAWN_USER_NAME);
        commentRepository.renameAuthor(userId, WITHDRAWN_USER_NAME);
        timelineEventRepository.findAllByActorId(userId)
                .forEach(event -> event.anonymizeActor(WITHDRAWN_USER_NAME));

        // 3) 본인 것은 삭제.
        List<ProjectFile> files = projectFileRepository.findAllByUploadedBy(userId);
        projectFileRepository.deleteAll(files);
        scheduleRepository.deleteAllByCreatedBy(userId);
        integrationRepository.deleteAllByConnectedById(userId);
        oauthExchangeCodeRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);

        // 4) DB 밖의 것들 — 되돌릴 수 없으니 DB 작업이 다 끝난 뒤 맨 마지막에.
        refreshTokenStore.invalidate(userId);
        for (ProjectFile file : files) {
            try {
                fileStorageService.delete(file.getFileKey());
            } catch (Exception e) {
                // 파일 하나를 못 지웠다고 탈퇴 전체를 실패시키지 않는다 — DB 행은 이미 지워져 접근할 수 없다.
                log.warn("탈퇴한 사용자의 업로드 파일 삭제 실패: {}", file.getFileKey(), e);
            }
        }
    }

    /** 비밀번호가 있는 계정은 비밀번호로 본인 확인 — 소셜 로그인 전용 계정은 확인할 비밀번호가 없어 건너뛴다. */
    private void verifyPassword(User user, DeleteAccountRequest request) {
        if (user.getPassword() == null) {
            return;
        }
        String password = request == null ? null : request.password();
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다.");
        }
    }
}
