package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.utp.estg.pp.printhub.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Integer userId);

    long countByUserIdAndReadFalse(Integer userId);

    Optional<Notification> findByIdAndUserId(Integer id, Integer userId);
}