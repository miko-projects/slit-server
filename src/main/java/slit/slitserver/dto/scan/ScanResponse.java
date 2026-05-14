package slit.slitserver.dto.scan;

import java.util.Map;

/**
 * Returned by POST /api/scan.
 * <p>
 * {@code scan} is the raw JSON object Claude returned — Flutter parses it with
 * its own {@code ReceiptScan.fromJson()}.  We pass it through as a Map so we
 * don't need to duplicate the full receipt schema in Java.
 */
public record ScanResponse(
        Map<String, Object> scan,
        int remainingCredits
) {}
