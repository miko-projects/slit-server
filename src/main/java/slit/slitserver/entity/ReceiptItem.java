package slit.slitserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "receipt_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReceiptItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;
    @Column(nullable = false) private String name;
    @Column(nullable = false, precision = 10, scale = 4) private BigDecimal qty;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false, precision = 10, scale = 2) private BigDecimal lineTotal;
    @Column(nullable = false) private String category = "pantry";
    @Column(nullable = false, precision = 4, scale = 3) private BigDecimal confidence;
    @Column(name = "qty_label") private String qtyLabel;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
}
