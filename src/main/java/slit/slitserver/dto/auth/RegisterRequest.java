package slit.slitserver.dto.auth;
import jakarta.validation.constraints.*;
public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String displayName,
    /** Chosen handle — 3-20 lowercase alphanumeric/underscore chars */
    @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[a-z0-9_]+$",
        message = "username may only contain lowercase letters, numbers and underscores")
    String username
) {}
