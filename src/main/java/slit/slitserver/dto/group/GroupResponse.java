package slit.slitserver.dto.group;
import java.time.Instant;
import java.util.*;
public record GroupResponse(
    UUID id, String name, String kind, String destination,
    Instant createdAt, List<GroupMemberResponse> members
) {}
