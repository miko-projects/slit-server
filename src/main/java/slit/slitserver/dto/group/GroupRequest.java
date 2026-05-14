package slit.slitserver.dto.group;
import jakarta.validation.constraints.*;
public record GroupRequest(
        @NotBlank String name,
        @NotBlank String kind,
        String destination,
        String currency          // ISO-4217, defaults to "USD" in service if null
) {}
