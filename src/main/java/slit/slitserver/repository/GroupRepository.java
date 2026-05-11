package slit.slitserver.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import slit.slitserver.entity.SlitGroup;
import java.util.*;
public interface GroupRepository extends JpaRepository<SlitGroup, UUID> {
    @Query("SELECT gm.group FROM GroupMember gm WHERE gm.user.id = :userId")
    List<SlitGroup> findAllByMemberUserId(UUID userId);
}
