package slit.slitserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    /** Chosen by user, not unique on its own — e.g. "mikos" */
    @Column(nullable = false)
    private String username;
    /** 4 uppercase hex chars, server-assigned — e.g. "A3F2" */
    @Column(nullable = false, length = 4)
    private String tag;
    @Column(name = "scan_credits", nullable = false)
    private int scanCredits = 3;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Full unique handle shown to other users, e.g. "mikos#A3F2" */
    public String getHandle() {
        return username + "#" + tag;
    }
}
