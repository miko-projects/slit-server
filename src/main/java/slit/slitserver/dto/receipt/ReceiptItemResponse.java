package slit.slitserver.dto.receipt;
import java.math.BigDecimal;
import java.util.UUID;
public record ReceiptItemResponse(
    UUID id, String name, BigDecimal qty, BigDecimal unitPrice,
    BigDecimal lineTotal, String category, BigDecimal confidence, String qtyLabel
) {}
