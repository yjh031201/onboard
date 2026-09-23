package com.kanban.backend.timeline;

import com.kanban.backend.timeline.dto.TimelineEventResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timeline")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    /** Recent activity history, most recent first — initial page load, before live WS updates take over. */
    @GetMapping
    public ResponseEntity<List<TimelineEventResponse>> recent(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(timelineService.recent(limit));
    }
}
