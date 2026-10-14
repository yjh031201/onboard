package com.kanban.backend.schedule;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /** 이 프로젝트에서 [from, to] 구간과 하루라도 겹치는 일정 — 전달에 시작해서 이번 달까지 이어지는 일정도 포함. */
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.projectId = :projectId AND s.startDate <= :to AND s.endDate >= :from
            ORDER BY s.startDate ASC, s.startTime ASC
            """)
    List<Schedule> findOverlapping(
            @Param("projectId") Long projectId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    void deleteAllByProjectId(Long projectId);

    /** 회원 탈퇴 시 — 본인이 만든 일정(모든 프로젝트). */
    void deleteAllByCreatedBy(Long createdBy);
}
