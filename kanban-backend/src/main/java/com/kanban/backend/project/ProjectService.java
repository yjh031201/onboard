package com.kanban.backend.project;

import com.kanban.backend.board.BoardColumn;
import com.kanban.backend.board.BoardColumnRepository;
import com.kanban.backend.board.CardRepository;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.label.Label;
import com.kanban.backend.label.LabelRepository;
import com.kanban.backend.project.dto.CreateProjectRequest;
import com.kanban.backend.project.dto.ProjectResponse;
import com.kanban.backend.project.dto.UpdateProjectRequest;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectAccessService projectAccessService;
    private final BoardColumnRepository boardColumnRepository;
    private final LabelRepository labelRepository;
    private final CardRepository cardRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectAccessService projectAccessService,
            BoardColumnRepository boardColumnRepository,
            LabelRepository labelRepository,
            CardRepository cardRepository,
            TimelineEventRepository timelineEventRepository,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectAccessService = projectAccessService;
        this.boardColumnRepository = boardColumnRepository;
        this.labelRepository = labelRepository;
        this.cardRepository = cardRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    /** 내가 (초대를 수락해서) 멤버인 프로젝트 목록 — 아직 수락 안 한 PENDING 초대는 여기 안 뜨고 /api/invitations에 뜬다. */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listMine(User actor) {
        return projectMemberRepository.findAllByUserIdAndStatus(actor.getId(), InviteStatus.ACCEPTED).stream()
                .map(member -> {
                    Project project = projectRepository.findById(member.getProjectId())
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
                    return ProjectResponse.from(project, member.getRole());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId, User actor) {
        Project project = findOrThrow(projectId);
        ProjectMember member = projectAccessService.requireMember(projectId, actor);
        return ProjectResponse.from(project, member.getRole());
    }

    /**
     * 게스트는 새 프로젝트를 만들 수 없음 (초대받아 멤버로만 참여 가능).
     * 만든 사람은 바로 OWNER가 되고, 기본 컬럼 3개 + 기본 라벨 5개가 같이 시드된다
     * (지금까지 단일 워크스페이스가 기본 제공하던 것과 동일).
     */
    @Transactional
    public ProjectResponse create(CreateProjectRequest request, User actor) {
        if (actor.isGuest()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "게스트 계정은 프로젝트를 만들 수 없어요. 초대를 받아 참여해주세요.");
        }

        Project project = projectRepository.save(new Project(request.name().trim(), request.description()));
        projectMemberRepository.save(new ProjectMember(project.getId(), actor.getId(), UserRole.OWNER));
        seedDefaults(project.getId());

        return ProjectResponse.from(project, UserRole.OWNER);
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request, User actor) {
        Project project = findOrThrow(projectId);
        ProjectMember member = projectAccessService.requireAdmin(projectId, actor);

        project.update(request.name().trim(), request.description());
        ProjectResponse response = ProjectResponse.from(project, member.getRole());
        realtimeEventPublisher.publish(RealtimeChannels.settingsEvents(projectId), response);
        return response;
    }

    /** 프로젝트를 읽기 전용으로 전환 — OWNER/ADMIN만, ArchiveGuardInterceptor가 실제로 쓰기를 막는다. */
    @Transactional
    public ProjectResponse archive(Long projectId, User actor) {
        Project project = findOrThrow(projectId);
        ProjectMember member = projectAccessService.requireAdmin(projectId, actor);

        project.archive();
        ProjectResponse response = ProjectResponse.from(project, member.getRole());
        realtimeEventPublisher.publish(RealtimeChannels.settingsEvents(projectId), response);
        return response;
    }

    @Transactional
    public ProjectResponse unarchive(Long projectId, User actor) {
        Project project = findOrThrow(projectId);
        ProjectMember member = projectAccessService.requireAdmin(projectId, actor);

        project.unarchive();
        ProjectResponse response = ProjectResponse.from(project, member.getRole());
        realtimeEventPublisher.publish(RealtimeChannels.settingsEvents(projectId), response);
        return response;
    }

    /**
     * 프로젝트를 영구 삭제 — OWNER만. 이 프로젝트의 카드(+라벨 연결)·컬럼·라벨·타임라인·멤버십을 전부 지우고
     * 프로젝트 자체를 삭제한다. 일정·파일은 아직 프로젝트별로 나뉘어 있지 않아(전역) 건드리지 않는다.
     */
    @Transactional
    public void delete(Long projectId, User actor) {
        findOrThrow(projectId);
        projectAccessService.requireOwner(projectId, actor);

        cardRepository.deleteAllByProjectId(projectId);
        boardColumnRepository.deleteAllByProjectId(projectId);
        labelRepository.deleteAllByProjectId(projectId);
        timelineEventRepository.deleteAllByProjectId(projectId);
        projectMemberRepository.deleteAllByProjectId(projectId);
        projectRepository.deleteById(projectId);
    }

    /** 새 프로젝트에 기본 컬럼 3개 + 기본 라벨 5개를 넣는다 — V11/V12 마이그레이션의 기본값과 동일. */
    public void seedDefaults(Long projectId) {
        boardColumnRepository.save(new BoardColumn(nextId(), projectId, "할 일", "#94a3b8", 0));
        boardColumnRepository.save(new BoardColumn(nextId(), projectId, "진행 중", "#f59e0b", 1));
        boardColumnRepository.save(new BoardColumn(nextId(), projectId, "완료", "#10b981", 2));

        labelRepository.save(new Label(nextId(), projectId, "버그", "#ef4444", 0));
        labelRepository.save(new Label(nextId(), projectId, "기능", "#6366f1", 1));
        labelRepository.save(new Label(nextId(), projectId, "디자인", "#a855f7", 2));
        labelRepository.save(new Label(nextId(), projectId, "긴급", "#f59e0b", 3));
        labelRepository.save(new Label(nextId(), projectId, "기타", "#9ca3af", 4, true));
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."));
    }
}
