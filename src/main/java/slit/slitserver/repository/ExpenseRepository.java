package slit.slitserver.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import slit.slitserver.entity.Expense;
import java.util.*;
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
