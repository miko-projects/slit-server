package slit.slitserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.dto.friend.FriendResponse;
import slit.slitserver.service.FriendshipService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipService friendshipService;

    /** GET /api/friends — list all accepted friends */
    @GetMapping
    public List<FriendResponse> listFriends(@AuthenticationPrincipal UserDetails principal) {
        return friendshipService.listFriends(uid(principal));
    }

    /** GET /api/friends/pending — incoming + outgoing pending requests */
    @GetMapping("/pending")
    public List<FriendResponse> listPending(@AuthenticationPrincipal UserDetails principal) {
        return friendshipService.listPending(uid(principal));
    }

    /**
     * POST /api/friends/request
     * Body: plain-text handle, e.g.  mikos#A3F2
     */
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendResponse sendRequest(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody String handle) {
        return friendshipService.sendRequest(uid(principal), handle.trim());
    }

    /**
     * POST /api/friends/{id}/accept
     * Accept an incoming pending request.
     */
    @PostMapping("/{id}/accept")
    public FriendResponse accept(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        return friendshipService.respondToRequest(uid(principal), id, true);
    }

    /**
     * POST /api/friends/{id}/decline
     * Decline (delete) an incoming pending request.
     */
    @PostMapping("/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        friendshipService.respondToRequest(uid(principal), id, false);
    }

    /**
     * DELETE /api/friends/{id}
     * Remove an accepted friend or cancel an outgoing request.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        friendshipService.removeFriend(uid(principal), id);
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
