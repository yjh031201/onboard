package com.kanban.backend.settings;

import com.kanban.backend.board.CardRepository;
import com.kanban.backend.board.dto.BoardEvent;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.file.FileStorageService;
import com.kanban.backend.file.ProjectFile;
import com.kanban.backend.file.ProjectFileRepository;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.schedule.ScheduleRepository;
import com.kanban.backend.settings.dto.ProjectSettingsResponse;
import com.kanban.backend.timeline.TimelineEventRepository;
import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRole;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 설정 페이지 "프로젝트 삭제". 단일 워크스페이스 구조라 프로젝트 로우를 지우는 게 아니라
 * 프로젝트에 쌓인 데이터(카드·일정·파일·타임라인·프로젝트 이름/설명)를 모두 지워 빈 상태로 되돌린다.
 * 계정(팀원)과 보드 구성(컬럼·라벨)은 남긴다.
 */
@Service
public class ProjectDeletionService {

    private static final Logger log = LoggerFactory.getLogger(ProjectDeletionService.class);

    private final CardRepository cardRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProjectFileRepository projectFileRepository;
    private final FileStorageService fileStorageService;
    private final TimelineEventRepository timelineEventRepository;
    private final ProjectSettingsRepository settingsRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ProjectDeletionService(
            CardRepository cardRepository,
            ScheduleRepository scheduleRepository,
            ProjectFileRepository projectFileRepository,
            FileStorageService fileStorageService,
            TimelineEventRepository timelineEventRepository,
            ProjectSettingsRepository settingsRepository,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.cardRepository = cardRepository;
        this.scheduleRepository = scheduleRepository;
        this.projectFileRepository = projectFileRepository;
        this.fileStorageService = fileStorageService;
        this.timelineEventRepository = timelineEventRepository;
        this.settingsRepository = settingsRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public void deleteProject(User currentUser) {
        if (currentUser.getRole() != UserRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "프로젝트는 소유자만 삭제할 수 있습니다.");
        }

        List<String> fileKeys = projectFileRepository.findAll().stream().map(ProjectFile::getFileKey).toList();

        // deleteAllInBatch가 아니라 deleteAll: 카드 라벨(card_labels)처럼 엔티티에 딸린 로우도 같이 지워져야 한다.
        cardRepository.deleteAll();
        scheduleRepository.deleteAll();
        projectFileRepository.deleteAll();
        timelineEventRepository.deleteAll();
        settingsRepository.deleteAll();

        // 저장소의 실제 파일. 하나가 실패해도 나머지는 계속 지운다 — DB 로우는 이미 지워서 더는 내려받을 수 없다.
        for (String fileKey : fileKeys) {
            try {
                fileStorageService.delete(fileKey);
            } catch (RuntimeException e) {
                log.warn("프로젝트 삭제 중 파일을 지우지 못했습니다: {}", fileKey, e);
            }
        }

        log.info("프로젝트 데이터가 삭제되었습니다. userId={}", currentUser.getId());

        // 접속 중인 다른 사람 화면도 빈 보드·초기 설정으로 맞춘다.
        realtimeEventPublisher.publish(
                RealtimeChannels.BOARD_EVENTS, new BoardEvent("CARD_DELETED", List.of(), currentUser.getName()));
        realtimeEventPublisher.publish(RealtimeChannels.SETTINGS_EVENTS, ProjectSettingsResponse.empty());
    }
}
