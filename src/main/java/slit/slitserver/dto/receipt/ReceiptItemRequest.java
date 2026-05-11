package slit.slitserver.dto.receipt;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ReceiptItemRequest(
    @NotBlank String name,
    @NotNull @Positive BigDecimal qty,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    @NotNull @PositiveOrZero BigDecimal lineTotal,
    @NotBlank String category,
    @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidence,
    String qtyLabel
) {}
