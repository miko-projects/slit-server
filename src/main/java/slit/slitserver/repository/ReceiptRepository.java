package slit.slitserver.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import slit.slitserver.entity.Receipt;
import java.util.*;
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    List<Receipt> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Receipt> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
