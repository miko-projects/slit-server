package slit.slitserver.dto.expense;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public record ExpenseResponse(
    UUID id, UUID groupId, UUID receiptId,
    String title, BigDecimal amount, String currency,
    UUID payerId, String payerName,
    String splitType, Instant createdAt,
    List<SplitResponse> splits
) {
    public record SplitResponse(UUID userId, String displayName, BigDecimal amountOwed) {}
}
