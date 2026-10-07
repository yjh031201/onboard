package com.kanban.backend.realtime;

/**
 * Redis pub-sub channel names and the STOMP topics each one fans out to.
 * A publish on one app instance is relayed through Redis so every instance's
 * WebSocket clients receive it — this is what lets card moves, presence and
 * timeline events sync across a multi-instance deployment.
 *
 * Board/label/column/timeline/presence/settings events are all per-project (멀티프로젝트 지원) —
 * the channel/topic name is built from the projectId so clients only hear about the
 * project they're currently looking at. Presence used to be global (전체 접속자) but
 * that leaked other projects' members into a project's "팀원 현황" widget, so a user's
 * online/offline change is now broadcast once per project they're actually a member of
 * (PresenceService looks that membership up) instead of to one single global topic.
 */
public final class RealtimeChannels {

    public static String presenceEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":presence";
    }

    public static String presenceTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/presence";
    }

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

    /** 프로젝트 이름/설명/보관 상태가 바뀌면 — 설정 페이지와 ArchivedBanner가 듣는다. */
    public static String settingsEvents(Long projectId) {
        return "realtime:projects:" + projectId + ":settings";
    }

    public static String settingsTopic(Long projectId) {
        return "/topic/projects/" + projectId + "/settings";
    }

    private RealtimeChannels() {
    }
}
