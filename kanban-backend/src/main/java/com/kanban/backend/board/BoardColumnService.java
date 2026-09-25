package com.kanban.backend.board;

import com.kanban.backend.board.dto.ColumnOrderRequest;
import com.kanban.backend.board.dto.ColumnRequest;
import com.kanban.backend.board.dto.ColumnResponse;
import com.kanban.backend.common.ApiException;
import com.kanban.backend.realtime.RealtimeChannels;
import com.kanban.backend.realtime.RealtimeEventPublisher;
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

    public BoardColumnService(
            BoardColumnRepository columnRepository,
            CardRepository cardRepository,
            RealtimeEventPublisher realtimeEventPublisher
    ) {
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ColumnResponse> list() {
        return columnRepository.findAllByOrderByPositionAsc().stream()
                .map(ColumnResponse::from)
                .toList();
    }

    @Transactional
    public ColumnResponse create(ColumnRequest request) {
        List<BoardColumn> columns = columnRepository.findAllByOrderByPositionAsc();
        int position = columns.isEmpty() ? 0 : columns.get(columns.size() - 1).getPosition() + 1;
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        BoardColumn column = columnRepository.save(new BoardColumn(id, request.name().trim(), request.color(), position));
        broadcastColumns();
        return ColumnResponse.from(column);
    }

    @Transactional
    public ColumnResponse update(String id, ColumnRequest request) {
        BoardColumn column = findOrThrow(id);
        column.update(request.name().trim(), request.color());
        broadcastColumns();
        return ColumnResponse.from(column);
    }

    /**
     * 컬럼 순서를 통째로 바꾼다. 그 사이 다른 사람이 컬럼을 추가/삭제했다면 id 목록이 맞지 않으므로
     * 거절하고, 클라이언트는 다음 /topic/columns 목록으로 다시 맞춰진다.
     */
    @Transactional
    public List<ColumnResponse> reorder(ColumnOrderRequest request) {
        List<BoardColumn> columns = columnRepository.findAllByOrderByPositionAsc();
        Map<String, BoardColumn> byId = columns.stream()
                .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));

        List<String> ids = request.columnIds();
        if (ids.size() != columns.size() || !byId.keySet().equals(Set.copyOf(ids))) {
            throw new ApiException(HttpStatus.CONFLICT, "컬럼 목록이 바뀌었습니다. 새로고침 후 다시 시도해주세요.");
        }

        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).reposition(i);
        }
        broadcastColumns();
        return list();
    }

    /** 카드가 남아 있는 컬럼이나 마지막 하나 남은 컬럼은 지울 수 없다 — 카드가 갈 곳 없이 사라지는 것을 막는다. */
    @Transactional
    public void delete(String id) {
        BoardColumn column = findOrThrow(id);
        if (cardRepository.existsByStatus(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "카드가 있는 컬럼은 삭제할 수 없습니다. 카드를 먼저 옮기거나 삭제해주세요.");
        }
        if (columnRepository.count() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "컬럼은 최소 1개 이상 있어야 합니다.");
        }

        columnRepository.delete(column);
        broadcastColumns();
    }

    private BoardColumn findOrThrow(String id) {
        return columnRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "컬럼을 찾을 수 없습니다."));
    }

    /** 변경 후 전체 목록을 /topic/columns로 보낸다 — list()의 조회가 보류 중인 변경을 먼저 flush한다. */
    private void broadcastColumns() {
        realtimeEventPublisher.publish(RealtimeChannels.COLUMN_EVENTS, list());
    }
}
