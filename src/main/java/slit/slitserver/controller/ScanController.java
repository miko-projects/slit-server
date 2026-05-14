package slit.slitserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.dto.scan.ScanRequest;
import slit.slitserver.dto.scan.ScanResponse;
import slit.slitserver.service.ScanService;

import java.util.UUID;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    /**
     * POST /api/scan
     * <p>
     * Checks that the authenticated user has at least 1 scan credit,
     * forwards the image to Claude for OCR, deducts 1 credit on success,
     * and returns the parsed receipt JSON + remaining credit count.
     * <p>
     * Returns 402 Payment Required if the user has 0 credits.
     */
    @PostMapping
    public ScanResponse scan(
            @Valid @RequestBody ScanRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return scanService.scan(req, UUID.fromString(principal.getUsername()));
    }
}
