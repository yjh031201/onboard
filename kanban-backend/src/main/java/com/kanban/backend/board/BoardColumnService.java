package com.kanban.backend.board;

import com.kanban.backend.board.dto.ColumnOrderRequest;
import com.kanban.backend.board.dto.ColumnRequest;
import com.kanban.backend.board.dto.ColumnResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.project.ProjectAccessService;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
import com.kanban.backend.user.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardColumnService {

    private final BoardColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;
    private final ProjectAccessService projectAccessService;

    public BoardColumnService(
            BoardColumnRepository columnRepository,
            CardRepository cardRepository,
            RealtimeEventPublisher realtimeEventPublisher,
            ProjectAccessService projectAccessService
    ) {
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public List<ColumnResponse> list(Long projectId, User actor) {
        projectAccessService.requireMember(projectId, actor);
        return listInternal(projectId);
    }

    @Transactional
    public ColumnResponse create(Long projectId, ColumnRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        List<BoardColumn> columns = columnRepository.findAllByProjectIdOrderByPositionAsc(projectId);
        int position = columns.isEmpty() ? 0 : columns.get(columns.size() - 1).getPosition() + 1;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        BoardColumn column = columnRepository.save(new BoardColumn(id, projectId, request.name().trim(), request.color(), position));
        broadcastColumns(projectId);
        return ColumnResponse.from(column);
    }

    @Transactional
    public ColumnResponse update(Long projectId, String id, ColumnRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        BoardColumn column = findOrThrow(projectId, id);
        column.update(request.name().trim(), request.color());
        broadcastColumns(projectId);
        return ColumnResponse.from(column);
    }

    /**
     * 컬럼 순서를 통째로 바꾼다. 그 사이 다른 사람이 컬럼을 추가/삭제했다면 id 목록이 맞지 않으므로
     * 거절하고, 클라이언트는 다음 /topic/projects/{id}/columns 목록으로 다시 맞춰진다.
     */
    @Transactional
    public List<ColumnResponse> reorder(Long projectId, ColumnOrderRequest request, User actor) {
        projectAccessService.requireMember(projectId, actor);
        List<BoardColumn> columns = columnRepository.findAllByProjectIdOrderByPositionAsc(projectId);
        Map<String, BoardColumn> byId = columns.stream()
                .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));

        List<String> ids = request.columnIds();
        if (ids.size() != columns.size() || !byId.keySet().equals(Set.copyOf(ids))) {
            throw new ApiException(HttpStatus.CONFLICT, "컬럼 목록이 바뀌었습니다. 새로고침 후 다시 시도해주세요.");
        }

        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).reposition(i);
        }
        broadcastColumns(projectId);
        return listInternal(projectId);
    }

    /** 카드가 남아 있는 컬럼이나 마지막 하나 남은 컬럼은 지울 수 없다 — 카드가 갈 곳 없이 사라지는 것을 막는다. */
    @Transactional
    public void delete(Long projectId, String id, User actor) {
        projectAccessService.requireMember(projectId, actor);
        BoardColumn column = findOrThrow(projectId, id);
        if (cardRepository.existsByProjectIdAndStatus(projectId, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "카드가 있는 컬럼은 삭제할 수 없습니다. 카드를 먼저 옮기거나 삭제해주세요.");
        }
        if (columnRepository.countByProjectId(projectId) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "컬럼은 최소 1개 이상 있어야 합니다.");
        }

        columnRepository.delete(column);
        broadcastColumns(projectId);
    }

    private List<ColumnResponse> listInternal(Long projectId) {
        return columnRepository.findAllByProjectIdOrderByPositionAsc(projectId).stream()
                .map(ColumnResponse::from)
                .toList();
    }

    private BoardColumn findOrThrow(Long projectId, String id) {
        BoardColumn column = columnRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "컬럼을 찾을 수 없습니다."));
        if (!column.getProjectId().equals(projectId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "컬럼을 찾을 수 없습니다.");
        }
        return column;
    }

    /** 변경 후 전체 목록을 /topic/projects/{id}/columns로 보낸다 — list()의 조회가 보류 중인 변경을 먼저 flush한다. */
    private void broadcastColumns(Long projectId) {
        realtimeEventPublisher.publish(RealtimeChannels.columnEvents(projectId), listInternal(projectId));
    }
}
