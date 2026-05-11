package slit.slitserver.dto.receipt;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public record ReceiptRequest(
    @NotBlank String storeName,
    String storeLocation,
    @NotNull Instant purchasedAt,
    String currency,
    @NotNull @PositiveOrZero BigDecimal subtotal,
    @NotNull @PositiveOrZero BigDecimal tax,
    @NotNull @PositiveOrZero BigDecimal total,
    String saveTarget,
    String scanQuality,
    UUID groupId,
    @NotNull List<ReceiptItemRequest> items
) {}
