package slit.slitserver.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import slit.slitserver.dto.auth.AuthResponse;
import slit.slitserver.dto.auth.LoginRequest;
import slit.slitserver.dto.auth.RegisterRequest;
import slit.slitserver.entity.User;
import slit.slitserver.exception.ApiException;
import slit.slitserver.repository.UserRepository;
import slit.slitserver.security.JwtUtil;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final SecureRandom RNG = new SecureRandom();
    private static final int MAX_TAG_ATTEMPTS = 10;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        String tag = generateUniqueTag(req.username());
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName())
                .username(req.username())
                .tag(tag)
                .scanCredits(5)
                .build();
        userRepository.save(user);
        String token = jwtUtil.generate(user.getId(), user.getEmail());
        return toAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String token = jwtUtil.generate(user.getId(), user.getEmail());
        return toAuthResponse(token, user);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String generateUniqueTag(String username) {
        for (int i = 0; i < MAX_TAG_ATTEMPTS; i++) {
            // 4 uppercase hex chars: 0-9, A-F  → 65 536 combinations per username
            String tag = String.format("%04X", RNG.nextInt(0x10000));
            if (!userRepository.existsByUsernameAndTag(username, tag)) {
                return tag;
            }
        }
        throw new ApiException(HttpStatus.CONFLICT,
                "Username '" + username + "' has no available tags — try a different username");
    }

    private AuthResponse toAuthResponse(String token, User user) {
        return new AuthResponse(
                token, user.getId(), user.getEmail(),
                user.getDisplayName(), user.getHandle(),
                user.getScanCredits());
    }
}
