
package pt.utp.estg.pp.printhub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;

import java.util.List;
import java.util.Optional;

public interface CustomRequestRepository
        extends JpaRepository<CustomRequest, Integer> {

    // Obter pedidos por estado, ordenados pela data de criação
    List<CustomRequest> findByStatusOrderByCreatedAtAsc(
            CustomRequestStatus status
    );

    // Bloquear um pedido durante a aprovação ou rejeição
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM CustomRequest p WHERE p.id = :requestId")
    Optional<CustomRequest> findByIdForUpdate(
            @Param("requestId") Integer requestId
    );
}
