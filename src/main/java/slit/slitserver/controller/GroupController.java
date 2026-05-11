package slit.slitserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.dto.group.GroupRequest;
import slit.slitserver.dto.group.GroupResponse;
import slit.slitserver.service.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public List<GroupResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return groupService.listForUser(uid(principal));
    }

    @GetMapping("/{id}")
    public GroupResponse get(@PathVariable UUID id,
                             @AuthenticationPrincipal UserDetails principal) {
        return groupService.get(id, uid(principal));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest req,
                                                @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(201).body(groupService.create(req, uid(principal)));
    }

    @PostMapping("/{id}/members")
    public GroupResponse addMember(@PathVariable UUID id,
                                   @RequestParam String email,
                                   @AuthenticationPrincipal UserDetails principal) {
        return groupService.addMember(id, uid(principal), email);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id,
                                             @PathVariable UUID memberId,
                                             @AuthenticationPrincipal UserDetails principal) {
        groupService.removeMember(id, uid(principal), memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal UserDetails principal) {
        groupService.delete(id, uid(principal));
        return ResponseEntity.noContent().build();
    }

    private UUID uid(UserDetails principal) {
        return UUID.fromString(principal.getUsername());
    }
}
