package slit.slitserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "group_members")
@Getter @Setter @NoArgsConstructor
public class GroupMember {
    @EmbeddedId
    private GroupMemberId id = new GroupMemberId();
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId") @JoinColumn(name = "group_id")
    private SlitGroup group;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId") @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt = Instant.now();

    @Embeddable
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
    public static class GroupMemberId implements Serializable {
        @Column(name = "group_id") private UUID groupId;
        @Column(name = "user_id")  private UUID userId;
    }
}
