package com.kanban.backend.realtime;

import java.security.Principal;

/**
 * Identifies the authenticated user on a STOMP session. The name is the
 * user id (as a string) so it also works as the STOMP session's Principal
 * name for /user/** destinations, if those are needed later.
 */
public record StompPrincipal(Long userId, String userName) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
