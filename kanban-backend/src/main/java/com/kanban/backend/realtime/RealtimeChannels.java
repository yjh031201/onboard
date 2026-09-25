package com.kanban.backend.realtime;

/**
 * Redis pub-sub channel names and the STOMP topics each one fans out to.
 * A publish on one app instance is relayed through Redis so every instance's
 * WebSocket clients receive it — this is what lets card moves, presence and
 * timeline events sync across a multi-instance deployment.
 */
public final class RealtimeChannels {

    public static final String BOARD_EVENTS = "realtime:board";
    public static final String PRESENCE_EVENTS = "realtime:presence";
    public static final String TIMELINE_EVENTS = "realtime:timeline";
    public static final String TIMELINE_DELETED_EVENTS = "realtime:timeline-deleted";
    public static final String LABEL_EVENTS = "realtime:labels";
    public static final String COLUMN_EVENTS = "realtime:columns";

    public static final String BOARD_TOPIC = "/topic/board";
    public static final String PRESENCE_TOPIC = "/topic/presence";
    public static final String TIMELINE_TOPIC = "/topic/timeline";
    public static final String TIMELINE_DELETED_TOPIC = "/topic/timeline-deleted";
    public static final String LABEL_TOPIC = "/topic/labels";
    public static final String COLUMN_TOPIC = "/topic/columns";

    private RealtimeChannels() {
    }
}
