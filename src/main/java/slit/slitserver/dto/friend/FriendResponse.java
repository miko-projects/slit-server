package slit.slitserver.dto.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
        UUID   friendshipId,
        UUID   userId,
        String displayName,
        String handle,        // username#TAG
        String status,        // "accepted" | "pending"
        String direction,     // "incoming" | "outgoing"  (only meaningful when pending)
        Instant since
) {}
