package com.kanban.backend.timeline;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
