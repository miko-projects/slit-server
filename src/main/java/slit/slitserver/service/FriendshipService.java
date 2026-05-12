package slit.slitserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slit.slitserver.dto.friend.FriendResponse;
import slit.slitserver.entity.Friendship;
import slit.slitserver.entity.User;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.FriendshipRepository;
import slit.slitserver.repository.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(UUID currentUserId) {
        return friendshipRepository.findAcceptedByUserId(currentUserId)
                .stream()
                .map(f -> toResponse(f, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> listPending(UUID currentUserId) {
        List<FriendResponse> result = new ArrayList<>();
        friendshipRepository.findPendingIncoming(currentUserId)
                .forEach(f -> result.add(toResponse(f, currentUserId)));
        friendshipRepository.findPendingOutgoing(currentUserId)
                .forEach(f -> result.add(toResponse(f, currentUserId)));
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    /**
     * Send a friend request to the user identified by "username#TAG".
     */
    @Transactional
    public FriendResponse sendRequest(UUID currentUserId, String handle) {
        // parse handle
        String[] parts = handle.split("#", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid handle — expected format: username#TAG");
        }
        String username = parts[0];
        String tag = parts[1].toUpperCase();

        User addressee = userRepository.findByUsernameAndTag(username, tag)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "User '" + handle + "' not found"));

        if (addressee.getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot add yourself as a friend");
        }

        friendshipRepository.findBetween(currentUserId, addressee.getId())
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT,
                            "A friendship or request already exists with this user");
                });

        User requester = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

        Friendship f = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status("pending")
                .build();
        friendshipRepository.save(f);
        return toResponse(f, currentUserId);
    }

    /**
     * Accept or decline a pending incoming request.
     * @param accept true → accepted, false → delete the request
     */
    @Transactional
    public FriendResponse respondToRequest(UUID currentUserId, UUID friendshipId, boolean accept) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Friendship not found"));

        if (!f.getAddressee().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You are not the addressee of this request");
        }
        if (!"pending".equals(f.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Request is not pending");
        }

        if (accept) {
            f.setStatus("accepted");
            f.setUpdatedAt(Instant.now());
            friendshipRepository.save(f);
            return toResponse(f, currentUserId);
        } else {
            friendshipRepository.delete(f);
            return toResponse(f, currentUserId); // return snapshot before deletion
        }
    }

    /**
     * Remove an accepted friend or cancel an outgoing request.
     */
    @Transactional
    public void removeFriend(UUID currentUserId, UUID friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Friendship not found"));

        boolean isParty = f.getRequester().getId().equals(currentUserId)
                || f.getAddressee().getId().equals(currentUserId);
        if (!isParty) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not part of this friendship");
        }
        friendshipRepository.delete(f);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private FriendResponse toResponse(Friendship f, UUID currentUserId) {
        boolean iAmRequester = f.getRequester().getId().equals(currentUserId);
        User other = iAmRequester ? f.getAddressee() : f.getRequester();
        String direction = iAmRequester ? "outgoing" : "incoming";

        return new FriendResponse(
                f.getId(),
                other.getId(),
                other.getDisplayName(),
                other.getHandle(),
                f.getStatus(),
                direction,
                f.getUpdatedAt() != null ? f.getUpdatedAt() : f.getCreatedAt()
        );
    }
}
