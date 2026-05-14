package slit.slitserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slit.slitserver.dto.group.GroupMemberResponse;
import slit.slitserver.dto.group.GroupRequest;
import slit.slitserver.dto.group.GroupResponse;
import slit.slitserver.entity.GroupMember;
import slit.slitserver.entity.SlitGroup;
import slit.slitserver.entity.User;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.GroupMemberRepository;
import slit.slitserver.repository.GroupRepository;
import slit.slitserver.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GroupResponse> listForUser(UUID userId) {
        return groupRepository.findAllByMemberUserId(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID groupId, UUID userId) {
        SlitGroup group = findMemberGroup(groupId, userId);
        return toResponse(group);
    }

    @Transactional
    public GroupResponse create(GroupRequest req, UUID userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        SlitGroup group = SlitGroup.builder()
                .name(req.name())
                .kind(req.kind())
                .destination(req.destination())
                .currency(req.currency() != null ? req.currency().toUpperCase() : "USD")
                .createdBy(creator)
                .build();
        groupRepository.save(group);

        // creator is automatically a member
        GroupMember member = new GroupMember();
        member.setId(new GroupMember.GroupMemberId(group.getId(), userId));
        member.setGroup(group);
        member.setUser(creator);
        groupMemberRepository.save(member);

        return toResponse(group);
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, UUID requestingUserId, UUID newUserId) {
        SlitGroup group = findMemberGroup(groupId, requestingUserId);
        User newUser = userRepository.findById(newUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, newUser.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already a member");
        }

        GroupMember member = new GroupMember();
        member.setId(new GroupMember.GroupMemberId(groupId, newUser.getId()));
        member.setGroup(group);
        member.setUser(newUser);
        groupMemberRepository.save(member);

        return toResponse(group);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID requestingUserId, UUID targetUserId) {
        findMemberGroup(groupId, requestingUserId);
        GroupMember.GroupMemberId pk = new GroupMember.GroupMemberId(groupId, targetUserId);
        if (!groupMemberRepository.existsById(pk)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Member not found in this group");
        }
        groupMemberRepository.deleteById(pk);
    }

    @Transactional
    public void delete(UUID groupId, UUID userId) {
        SlitGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!group.getCreatedBy().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the group creator can delete it");
        }
        groupRepository.delete(group);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SlitGroup findMemberGroup(UUID groupId, UUID userId) {
        SlitGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Group not found"));
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return group;
    }

    public GroupResponse toResponse(SlitGroup g) {
        List<GroupMemberResponse> members = groupMemberRepository.findByGroupId(g.getId())
                .stream()
                .map(gm -> new GroupMemberResponse(
                        gm.getUser().getId(),
                        gm.getUser().getDisplayName(),
                        gm.getUser().getEmail()))
                .toList();
        return new GroupResponse(g.getId(), g.getName(), g.getKind(),
                g.getDestination(), g.getCurrency(), g.getCreatedAt(), members);
    }
}
