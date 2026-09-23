package com.kanban.backend.realtime.presence;

import com.kanban.backend.user.User;
import com.kanban.backend.user.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public PresenceController(UserRepository userRepository, PresenceService presenceService) {
        this.userRepository = userRepository;
        this.presenceService = presenceService;
    }

    /** Full team roster with current presence — used for the widget's initial paint before live WS deltas arrive. */
    @GetMapping
    public ResponseEntity<List<PresenceEvent>> roster() {
        Set<Long> onlineUserIds = presenceService.onlineUserIds();

        List<PresenceEvent> roster = userRepository.findAll().stream()
                .map(user -> toPresenceEvent(user, onlineUserIds))
                .toList();

        return ResponseEntity.ok(roster);
    }

    private PresenceEvent toPresenceEvent(User user, Set<Long> onlineUserIds) {
        PresenceStatus status = onlineUserIds.contains(user.getId()) ? PresenceStatus.ONLINE : PresenceStatus.OFFLINE;
        return new PresenceEvent(user.getId(), user.getName(), status);
    }
}
