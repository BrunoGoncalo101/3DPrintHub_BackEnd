package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;

import java.util.List;

public interface CustomRequestRepository extends JpaRepository<CustomRequest, Integer> {
    List<CustomRequest> findByStatusOrderByCreatedAtAsc(CustomRequestStatus status);
}
