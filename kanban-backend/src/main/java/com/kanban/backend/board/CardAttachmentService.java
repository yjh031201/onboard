package com.kanban.backend.board;

import com.kanban.backend.board.dto.AttachRequest;
import com.kanban.backend.board.dto.CardResponse;
import com.kanban.backend.board.dto.ImportGithubIssuesRequest;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.integration.drive.DriveIntegrationService;
import com.kanban.backend.integration.drive.dto.DriveFileResponse;
import com.kanban.backend.integration.github.GithubIntegrationService;
import com.kanban.backend.integration.github.dto.GithubIssueDetail;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.timeline.TimelineEventType;
import com.kanban.backend.timeline.TimelineService;
import com.kanban.backend.user.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연동(GitHub/Google Drive)과 칸반 카드를 잇는다.
 * - GitHub 이슈 → 카드로 가져오기 (가져온 카드에는 원본 이슈 링크가 첨부로 붙는다)
 * - 카드에 GitHub 이슈 / Drive 파일 링크 첨부·삭제
 * 제목·주소는 항상 서버가 GitHub/Drive API에서 직접 받아 저장한다 — 클라이언트는 이슈 번호·파일 id만 보낸다.
 */
@Service
public class CardAttachmentService {

    /** 카드 하나에 붙일 수 있는 첨부 수. */
    public static final int MAX_ATTACHMENTS_PER_CARD = 10;

    private final CardService cardService;
    private final CardAttachmentRepository attachmentRepository;
    private final GithubIntegrationService githubIntegrationService;
    private final DriveIntegrationService driveIntegrationService;
    private final TimelineService timelineService;
    private final ProjectAccessService projectAccessService;

    public CardAttachmentService(
            CardService cardService,
            CardAttachmentRepository attachmentRepository,
            GithubIntegrationService githubIntegrationService,
            DriveIntegrationService driveIntegrationService,
            TimelineService timelineService,
            ProjectAccessService projectAccessService
    ) {
        this.cardService = cardService;
        this.attachmentRepository = attachmentRepository;
        this.githubIntegrationService = githubIntegrationService;
        this.driveIntegrationService = driveIntegrationService;
        this.timelineService = timelineService;
        this.projectAccessService = projectAccessService;
    }

    /** 프로젝트 멤버면 누구나 첨부할 수 있다 (라벨 붙이기와 같은 기준). */
    @Transactional
    public CardResponse attach(Long projectId, Long cardId, AttachRequest request, User actor) {
        Card card = cardService.requireCard(projectId, cardId, actor);
        if (attachmentRepository.countByCardId(cardId) >= MAX_ATTACHMENTS_PER_CARD) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "첨부는 카드당 %d개까지 붙일 수 있습니다.".formatted(MAX_ATTACHMENTS_PER_CARD));
        }

        CardAttachment attachment = switch (request.kind()) {
            case GITHUB_ISSUE -> githubIssueAttachment(card, parseIssueNumber(request.ref()), actor);
            case DRIVE_FILE -> driveFileAttachment(card, request.ref().strip(), actor);
        };
        if (attachmentRepository.existsByCardIdAndKindAndExternalId(cardId, attachment.getKind(), attachment.getExternalId())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 이 카드에 첨부된 항목이에요.");
        }
        attachmentRepository.save(attachment);

        String what = attachment.getKind() == AttachmentKind.GITHUB_ISSUE ? "GitHub 이슈" : "Drive 파일";
        timelineService.record(
                projectId,
                TimelineEventType.CARD_UPDATED,
                "%s님이 '%s' 카드에 %s '%s'을(를) 첨부했습니다"
                        .formatted(actor.getName(), card.getTitle(), what, attachment.getTitle()),
                actor,
                false
        );
        cardService.broadcastSnapshot(projectId, "CARD_UPDATED", actor.getName());
        return cardService.toResponse(card);
    }

    /** 첨부한 본인, 또는 카드를 수정할 수 있는 사람(작성자·OWNER·ADMIN)이 뗄 수 있다. */
    @Transactional
    public CardResponse detach(Long projectId, Long cardId, Long attachmentId, User actor) {
        Card card = cardService.requireCard(projectId, cardId, actor);
        CardAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(found -> found.getCardId().equals(cardId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "첨부를 찾을 수 없습니다."));
        if (!attachment.getAttachedById().equals(actor.getId()) && !cardService.isManageable(projectId, card, actor)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인이 붙인 첨부만 뗄 수 있습니다.");
        }

        attachmentRepository.delete(attachment);
        cardService.broadcastSnapshot(projectId, "CARD_UPDATED", actor.getName());
        return cardService.toResponse(card);
    }

    /**
     * 연결된 저장소의 이슈들을 status 컬럼 맨 아래에 카드로 만든다. 제목은 "#번호 이슈 제목", 설명은 이슈 본문.
     * 이 프로젝트에 이미 가져온(또는 첨부된) 이슈는 건너뛴다. 타임라인·Slack 알림은 한 번만 보낸다.
     */
    @Transactional
    public List<CardResponse> importGithubIssues(Long projectId, ImportGithubIssuesRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        String repoFullName = githubIntegrationService.linkedRepoFullName();

        List<Card> created = new ArrayList<>();
        for (Long number : request.issueNumbers().stream().distinct().toList()) {
            if (number == null || number <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "올바르지 않은 이슈 번호입니다.");
            }
            String externalId = issueExternalId(repoFullName, number);
            if (attachmentRepository.existsByProjectIdAndKindAndExternalId(projectId, AttachmentKind.GITHUB_ISSUE, externalId)) {
                continue;
            }

            GithubIssueDetail issue = githubIntegrationService.getIssue(number);
            String issueTitle = "#%d %s".formatted(issue.number(), issue.title());
            Card card = cardService.createImported(
                    projectId,
                    request.status(),
                    truncate(issueTitle, 200),
                    issue.body().isBlank() ? null : truncate(issue.body().strip(), 2000),
                    actor
            );
            attachmentRepository.save(new CardAttachment(
                    card.getId(), projectId, AttachmentKind.GITHUB_ISSUE, externalId,
                    issueTitle, issue.htmlUrl(), null, actor.getId(), actor.getName()
            ));
            created.add(card);
        }

        if (created.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "선택한 이슈는 이미 모두 카드로 가져왔어요.");
        }

        timelineService.record(
                projectId,
                TimelineEventType.CARD_CREATED,
                "%s님이 GitHub 이슈 %d개를 카드로 가져왔습니다".formatted(actor.getName(), created.size()),
                actor,
                true
        );
        cardService.broadcastSnapshot(projectId, "CARD_CREATED", actor.getName());
        return created.stream().map(cardService::toResponse).toList();
    }

    private CardAttachment githubIssueAttachment(Card card, long number, User actor) {
        String repoFullName = githubIntegrationService.linkedRepoFullName();
        GithubIssueDetail issue = githubIntegrationService.getIssue(number);
        return new CardAttachment(
                card.getId(), card.getProjectId(), AttachmentKind.GITHUB_ISSUE, issueExternalId(repoFullName, number),
                "#%d %s".formatted(issue.number(), issue.title()), issue.htmlUrl(), null,
                actor.getId(), actor.getName()
        );
    }

    private CardAttachment driveFileAttachment(Card card, String fileId, User actor) {
        DriveFileResponse file = driveIntegrationService.getFile(fileId);
        if (file.webViewLink().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "링크로 열 수 없는 Drive 항목이에요.");
        }
        return new CardAttachment(
                card.getId(), card.getProjectId(), AttachmentKind.DRIVE_FILE, file.id(),
                file.name().isBlank() ? "(이름 없음)" : file.name(), file.webViewLink(),
                file.mimeType().isBlank() ? null : file.mimeType(),
                actor.getId(), actor.getName()
        );
    }

    private static long parseIssueNumber(String ref) {
        try {
            long number = Long.parseLong(ref.strip().replaceFirst("^#", ""));
            if (number > 0) {
                return number;
            }
        } catch (NumberFormatException ignored) {
            // 아래에서 같은 오류로 처리
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "올바르지 않은 이슈 번호입니다.");
    }

    /** 저장소를 바꿔도 다른 저장소의 같은 번호 이슈와 섞이지 않게 저장소 이름을 붙인다. */
    static String issueExternalId(String repoFullName, long number) {
        return repoFullName + "#" + number;
    }

    private static String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
