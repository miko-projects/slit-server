package slit.slitserver.dto.auth;
import java.util.UUID;
public record AuthResponse(
    String token,
    UUID   userId,
    String email,
    String displayName,
    String handle,       // username#TAG  — share this to receive friend requests
    int    scanCredits
) {}
