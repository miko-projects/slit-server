package slit.slitserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import slit.slitserver.entity.GroupMember;

import java.util.List;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMember.GroupMemberId> {

    @Query("SELECT gm FROM GroupMember gm WHERE gm.id.groupId = :groupId")
    List<GroupMember> findByGroupId(UUID groupId);

    @Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.id.groupId = :groupId AND gm.id.userId = :userId")
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}
