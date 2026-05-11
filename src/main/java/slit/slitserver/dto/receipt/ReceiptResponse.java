package slit.slitserver.dto.receipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public record ReceiptResponse(
    UUID id, String storeName, String storeLocation,
    Instant purchasedAt, String currency,
    BigDecimal subtotal, BigDecimal tax, BigDecimal total,
    String saveTarget, String scanQuality,
    UUID groupId, Instant createdAt,
    List<ReceiptItemResponse> items
) {}
