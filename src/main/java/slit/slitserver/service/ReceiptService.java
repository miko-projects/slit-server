package slit.slitserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slit.slitserver.dto.receipt.*;
import slit.slitserver.entity.*;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<ReceiptResponse> listForUser(UUID userId) {
        return receiptRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse get(UUID id, UUID userId) {
        return toResponse(findOwned(id, userId));
    }

    @Transactional
    public ReceiptResponse create(ReceiptRequest req, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        SlitGroup group = resolveGroup(req.groupId());

        Receipt receipt = Receipt.builder()
                .user(user)
                .group(group)
                .storeName(req.storeName())
                .storeLocation(req.storeLocation())
                .purchasedAt(req.purchasedAt())
                .currency(req.currency() != null ? req.currency() : "USD")
                .subtotal(req.subtotal())
                .tax(req.tax())
                .total(req.total())
                .saveTarget(req.saveTarget() != null ? req.saveTarget() : "personal")
                .scanQuality(req.scanQuality() != null ? req.scanQuality() : "clear")
                .build();

        populateItems(receipt, req.items());
        return toResponse(receiptRepository.save(receipt));
    }

    @Transactional
    public ReceiptResponse update(UUID id, ReceiptRequest req, UUID userId) {
        Receipt receipt = findOwned(id, userId);
        receipt.setGroup(resolveGroup(req.groupId()));
        receipt.setStoreName(req.storeName());
        receipt.setStoreLocation(req.storeLocation());
        receipt.setPurchasedAt(req.purchasedAt());
        if (req.currency() != null) receipt.setCurrency(req.currency());
        receipt.setSubtotal(req.subtotal());
        receipt.setTax(req.tax());
        receipt.setTotal(req.total());
        if (req.saveTarget() != null) receipt.setSaveTarget(req.saveTarget());
        if (req.scanQuality() != null) receipt.setScanQuality(req.scanQuality());
        receipt.getItems().clear();
        populateItems(receipt, req.items());
        return toResponse(receiptRepository.save(receipt));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        receiptRepository.delete(findOwned(id, userId));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SlitGroup resolveGroup(UUID groupId) {
        if (groupId == null) return null;
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private Receipt findOwned(UUID id, UUID userId) {
        Receipt r = receiptRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Receipt not found"));
        if (!r.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return r;
    }

    private void populateItems(Receipt receipt, List<ReceiptItemRequest> itemRequests) {
        AtomicInteger order = new AtomicInteger(0);
        itemRequests.stream()
                .map(ir -> {
                    ReceiptItem item = new ReceiptItem();
                    item.setReceipt(receipt);
                    item.setName(ir.name());
                    item.setQty(ir.qty() != null ? ir.qty() : BigDecimal.ONE);
                    item.setUnitPrice(ir.unitPrice());
                    item.setLineTotal(ir.lineTotal());
                    item.setCategory(ir.category() != null ? ir.category() : "other");
                    item.setConfidence(ir.confidence() != null ? ir.confidence() : BigDecimal.ONE);
                    item.setQtyLabel(ir.qtyLabel());
                    item.setSortOrder(order.getAndIncrement());
                    return item;
                })
                .forEach(receipt.getItems()::add);
    }

    public ReceiptResponse toResponse(Receipt r) {
        List<ReceiptItemResponse> items = r.getItems().stream()
                .map(it -> new ReceiptItemResponse(
                        it.getId(), it.getName(), it.getQty(),
                        it.getUnitPrice(), it.getLineTotal(),
                        it.getCategory(), it.getConfidence(),
                        it.getQtyLabel()))
                .toList();
        return new ReceiptResponse(
                r.getId(),
                r.getStoreName(), r.getStoreLocation(),
                r.getPurchasedAt(), r.getCurrency(),
                r.getSubtotal(), r.getTax(), r.getTotal(),
                r.getSaveTarget(), r.getScanQuality(),
                r.getGroup() != null ? r.getGroup().getId() : null,
                r.getCreatedAt(), items);
    }
}
