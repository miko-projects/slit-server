package slit.slitserver.dto.expense;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public record ExpenseSplitRequest(@NotNull UUID userId, @NotNull @PositiveOrZero BigDecimal amountOwed) {}
