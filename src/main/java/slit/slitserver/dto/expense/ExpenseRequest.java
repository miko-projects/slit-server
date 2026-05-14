package slit.slitserver.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseRequest(
        @NotBlank String title,
        @NotNull @Positive BigDecimal amount,
        @NotNull UUID payerId,
        String splitType,
        String currency,          // ISO-4217, defaults to group currency if null
        UUID receiptId,
        List<ExpenseSplitRequest> splits
) {
}
