package slit.slitserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import slit.slitserver.dto.scan.ScanRequest;
import slit.slitserver.dto.scan.ScanResponse;
import slit.slitserver.entity.User;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    // ── Paste your Anthropic API key here or set ANTHROPIC_API_KEY env var ────
    // Get yours at https://console.anthropic.com → API Keys
    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private final UserRepository userRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL          = "claude-haiku-4-5-20251001";

    // ── Same prompt as the Flutter client ─────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
You are a receipt parser for a personal finance app.
Return ONLY a JSON object that matches the schema. No prose, no markdown.

Rules:
- Extract every visible line item, even if partially occluded.
- qty defaults to 1 unless explicitly listed (e.g. "2 @ $1.49").
- For weighed items, use the displayed total weight as qty and unit as price/unit.
- Categorize each product using: produce, dairy, bakery, pantry, meat, alcohol,
  beverages, household, personal, snacks, frozen. Use "pantry" as the fallback.
  Use the store type, location, and receipt context to improve categorization.
- confidence in [0, 1]. Drop below 0.7 when text is partial, blurred, or you
  guessed any field.
- If the receipt is crumpled / cropped / blurry, still return your best
  partial extraction and add notes to warnings[].
- Currency = ISO-4217 (default USD). Amounts in major units (12.34, not 1234).
- Detect the language/locale from the store name, address, or receipt text.
  Translate all item names into English.
- PRODUCTS ONLY: include only actual purchased products in "items".
  Exclude taxes, VAT lines, loyalty points, bag fees, deposits, subtotals,
  totals, cashier notes, receipt numbers, store slogans.
- DISCOUNTS: if a discount line immediately follows a product and reduces its
  price, apply the discount to that product's unit_price and line_total.
  Do not include discounts as separate items.
  If a discount is a general basket discount, skip it entirely.

Schema:
{
  "store_name": string,
  "store_location": string|null,
  "purchased_at": ISO-8601 string,
  "currency": "USD" | ...,
  "subtotal": number, "tax": number, "total": number,
  "items": [{"name": string, "qty": number, "unit_price": number,
             "line_total": number, "category": string,
             "confidence": number}],
  "scan_quality": "clear"|"partial"|"blurry"|"crumpled",
  "warnings": [string]
}
""";

    // ── Main method ───────────────────────────────────────────────────────────

    @Transactional
    public ScanResponse scan(ScanRequest req, UUID userId) {

        // ── 1. Check credits ────────────────────────────────────────────────
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getScanCredits() <= 0) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "You have no scan credits left. Purchase more to keep scanning.");
        }

        // ── 2. Validate API key ─────────────────────────────────────────────
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Scan service not configured (missing API key).");
        }

        // ── 3. Call Claude ──────────────────────────────────────────────────
        String mediaType = req.mediaType() != null ? req.mediaType() : "image/jpeg";

        Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "max_tokens", 4096,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "image",
                                        "source", Map.of(
                                                "type", "base64",
                                                "media_type", mediaType,
                                                "data", req.base64Image()
                                        )
                                ),
                                Map.of(
                                        "type", "text",
                                        "text", "Parse this receipt. Return only the JSON object."
                                )
                        )
                ))
        );

        AnthropicResponse anthropicResp;
        try {
            anthropicResp = restClient.post()
                    .uri(ANTHROPIC_URL)
                    .header("x-api-key", anthropicApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(AnthropicResponse.class);
        } catch (Exception e) {
            log.error("Anthropic API call failed: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Receipt scan failed: " + e.getMessage());
        }

        if (anthropicResp == null || anthropicResp.content() == null || anthropicResp.content().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from scan service.");
        }

        // ── 4. Extract JSON from Claude's text ──────────────────────────────
        String text = anthropicResp.content().stream()
                .filter(b -> "text".equals(b.type()))
                .map(ContentBlock::text)
                .reduce("", String::concat);

        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start < 0 || end < 0) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Scan service returned an unrecognised response.");
        }

        Map<String, Object> scanJson;
        try {
            scanJson = objectMapper.readValue(
                    text.substring(start, end + 1),
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Could not parse scan result: " + e.getMessage());
        }

        // ── 5. Deduct 1 credit ──────────────────────────────────────────────
        user.setScanCredits(user.getScanCredits() - 1);
        userRepository.save(user);

        log.info("Scan complete for user {}. Credits remaining: {}", userId, user.getScanCredits());

        return new ScanResponse(scanJson, user.getScanCredits());
    }

    // ── Inner DTOs for Anthropic response ─────────────────────────────────────

    record AnthropicResponse(
            List<ContentBlock> content
    ) {}

    record ContentBlock(
            String type,
            String text
    ) {}
}
