package slit.slitserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import slit.slitserver.entity.User;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/api/users/me")
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails principal) {
        User user = findUser(principal);
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "handle", user.getHandle(),
                "scanCredits", user.getScanCredits(),
                "createdAt", user.getCreatedAt()
        );
    }

    @GetMapping("/api/users/me/credits")
    public Map<String, Integer> credits(@AuthenticationPrincipal UserDetails principal) {
        User user = findUser(principal);
        return Map.of("scanCredits", user.getScanCredits());
    }

    /**
     * Deduct one scan credit. The Flutter app calls this when real OCR is used.
     * Returns the updated credit balance.
     */
    @PostMapping("/api/scan/use")
    public ResponseEntity<Map<String, Integer>> useScanCredit(
            @AuthenticationPrincipal UserDetails principal) {
        User user = findUser(principal);
        if (user.getScanCredits() <= 0) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "No scan credits remaining");
        }
        user.setScanCredits(user.getScanCredits() - 1);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("scanCredits", user.getScanCredits()));
    }

    private User findUser(UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
