package slit.slitserver.dto.scan;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(
        @NotBlank String base64Image,
        String mediaType   // defaults to "image/jpeg" in service if null
) {}
