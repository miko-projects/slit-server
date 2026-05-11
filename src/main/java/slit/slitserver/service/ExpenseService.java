package slit.slitserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slit.slitserver.dto.expense.ExpenseRequest;
import slit.slitserver.dto.expense.ExpenseResponse;
import slit.slitserver.entity.*;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ReceiptRepository receiptRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> listForGroup(UUID groupId, UUID userId) {
        requireMembership(groupId, userId);
        return expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExpenseResponse create(UUID groupId, ExpenseRequest req, UUID userId) {
        requireMembership(groupId, userId);

        SlitGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
        User payer = userRepository.findById(req.payerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payer not found"));

        Receipt receipt = null;
        if (req.receiptId() != null) {
            receipt = receiptRepository.findById(req.receiptId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Receipt not found"));
        }

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setReceipt(receipt);
        expense.setTitle(req.title());
        expense.setAmount(req.amount());
        expense.setPayer(payer);
        expense.setSplitType(req.splitType() != null ? req.splitType() : "equal");

        buildSplits(expense, req, groupId);
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(UUID groupId, UUID expenseId, UUID userId) {
        requireMembership(groupId, userId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
        if (!expense.getGroup().getId().equals(groupId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Expense does not belong to this group");
        }
        expenseRepository.delete(expense);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void requireMembership(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }
    }

    private void buildSplits(Expense expense, ExpenseRequest req, UUID groupId) {
        String splitType = expense.getSplitType();
        if ("equal".equals(splitType)) {
            // split equally among all group members
            List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
            int count = members.size();
            BigDecimal share = expense.getAmount()
                    .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            for (GroupMember gm : members) {
                ExpenseSplit split = new ExpenseSplit();
                split.setExpense(expense);
                split.setUser(gm.getUser());
                split.setAmountOwed(share);
                expense.getSplits().add(split);
            }
        } else if ("custom".equals(splitType) && req.splits() != null) {
            for (var sr : req.splits()) {
                User u = userRepository.findById(sr.userId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found in split: " + sr.userId()));
                ExpenseSplit split = new ExpenseSplit();
                split.setExpense(expense);
                split.setUser(u);
                split.setAmountOwed(sr.amountOwed());
                expense.getSplits().add(split);
            }
        }
    }

    private ExpenseResponse toResponse(Expense e) {
        List<ExpenseResponse.SplitResponse> splits = e.getSplits().stream()
                .map(s -> new ExpenseResponse.SplitResponse(
                        s.getUser().getId(),
                        s.getUser().getDisplayName(),
                        s.getAmountOwed()))
                .toList();
        return new ExpenseResponse(
                e.getId(),
                e.getGroup().getId(),
                e.getReceipt() != null ? e.getReceipt().getId() : null,
                e.getTitle(), e.getAmount(),
                e.getPayer().getId(), e.getPayer().getDisplayName(),
                e.getSplitType(), e.getCreatedAt(), splits);
    }
}
