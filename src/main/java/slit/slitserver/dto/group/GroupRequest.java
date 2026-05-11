package slit.slitserver.dto.group;
import jakarta.validation.constraints.*;
public record GroupRequest(@NotBlank String name, @NotBlank String kind, String destination) {}
