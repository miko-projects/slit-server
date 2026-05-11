package slit.slitserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import slit.slitserver.entity.Friendship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /** All accepted friendships involving this user (either side). */
    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = 'accepted'
              AND (f.requester.id = :userId OR f.addressee.id = :userId)
            """)
    List<Friendship> findAcceptedByUserId(@Param("userId") UUID userId);

    /** Pending requests where this user is the addressee (incoming). */
    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = 'pending'
              AND f.addressee.id = :userId
            """)
    List<Friendship> findPendingIncoming(@Param("userId") UUID userId);

    /** Pending requests this user sent (outgoing). */
    @Query("""
            SELECT f FROM Friendship f
            WHERE f.status = 'pending'
              AND f.requester.id = :userId
            """)
    List<Friendship> findPendingOutgoing(@Param("userId") UUID userId);

    /** Any friendship record between two users (regardless of direction). */
    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requester.id = :a AND f.addressee.id = :b)
               OR (f.requester.id = :b AND f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);
}
