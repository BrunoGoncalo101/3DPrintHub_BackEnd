package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.utp.estg.pp.printhub.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByResetTokenHash(String resetTokenHash);
}
