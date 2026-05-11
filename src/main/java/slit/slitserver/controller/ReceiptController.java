package slit.slitserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.dto.receipt.ReceiptRequest;
import slit.slitserver.dto.receipt.ReceiptResponse;
import slit.slitserver.service.ReceiptService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping
    public List<ReceiptResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return receiptService.listForUser(UUID.fromString(principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ReceiptResponse get(@PathVariable UUID id,
                               @AuthenticationPrincipal UserDetails principal) {
        return receiptService.get(id, UUID.fromString(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<ReceiptResponse> create(@Valid @RequestBody ReceiptRequest req,
                                                  @AuthenticationPrincipal UserDetails principal) {
        ReceiptResponse body = receiptService.create(req, UUID.fromString(principal.getUsername()));
        return ResponseEntity.status(201).body(body);
    }

    @PutMapping("/{id}")
    public ReceiptResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody ReceiptRequest req,
                                  @AuthenticationPrincipal UserDetails principal) {
        return receiptService.update(id, req, UUID.fromString(principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal UserDetails principal) {
        receiptService.delete(id, UUID.fromString(principal.getUsername()));
        return ResponseEntity.noContent().build();
    }
}
