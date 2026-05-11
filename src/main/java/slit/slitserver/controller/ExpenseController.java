package slit.slitserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.dto.expense.ExpenseRequest;
import slit.slitserver.dto.expense.ExpenseResponse;
import slit.slitserver.service.ExpenseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public List<ExpenseResponse> list(@PathVariable UUID groupId,
                                      @AuthenticationPrincipal UserDetails principal) {
        return expenseService.listForGroup(groupId, uid(principal));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@PathVariable UUID groupId,
                                                  @Valid @RequestBody ExpenseRequest req,
                                                  @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(201).body(expenseService.create(groupId, req, uid(principal)));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(@PathVariable UUID groupId,
                                       @PathVariable UUID expenseId,
                                       @AuthenticationPrincipal UserDetails principal) {
        expenseService.delete(groupId, expenseId, uid(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
