package com.kanban.backend.realtime;

/**
 * Redis pub-sub channel names and the STOMP topics each one fans out to.
 * A publish on one app instance is relayed through Redis so every instance's
 * WebSocket clients receive it — this is what lets card moves, presence and
 * timeline events sync across a multi-instance deployment.
 *
 * Board/label/column/timeline events are now per-project (멀티프로젝트 지원) — the
 * channel/topic name is built from the projectId so clients only hear about the
 * project they're currently looking at. Presence stays global (전체 접속자), since
 * "누가 지금 온라인인지"는 프로젝트 구분 없이 하나의 개념이다.
 */
public final class RealtimeChannels {

    public static final String PRESENCE_EVENTS = "realtime:presence";
    public static final String PRESENCE_TOPIC = "/topic/presence";

    public static String boardEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":board";
    }

    public static String boardTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/board";
    }

    public static String timelineEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":timeline";
    }

    public static String timelineTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/timeline";
    }

    public static String timelineDeletedEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":timeline-deleted";
    }

    public static String timelineDeletedTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/timeline-deleted";
    }

    public static String labelEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":labels";
    }

    public static String labelTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/labels";
    }

    public static String columnEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":columns";
    }

    public static String columnTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/columns";
    }

    private RealtimeChannels() {
    }
}
