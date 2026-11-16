package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.utp.estg.pp.printhub.entity.Order;
import pt.utp.estg.pp.printhub.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    Optional<Order> findByIdAndUserId(Integer id, Integer userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Integer userId);

    // Pesquisa para o admin: cada filtro só é aplicado se vier preenchido
    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:from IS NULL OR o.createdAt >= :from)
              AND (:to IS NULL OR o.createdAt < :to)
            ORDER BY o.createdAt DESC
            """)
    List<Order> search(@Param("status") OrderStatus status,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to);
}