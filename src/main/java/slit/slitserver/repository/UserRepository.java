package slit.slitserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import slit.slitserver.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndTag(String username, String tag);
    Optional<User> findByUsernameAndTag(String username, String tag);
}
