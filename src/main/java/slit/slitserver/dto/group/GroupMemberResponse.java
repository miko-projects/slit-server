package slit.slitserver.dto.group;
import java.util.UUID;
public record GroupMemberResponse(UUID userId, String displayName, String email) {}
