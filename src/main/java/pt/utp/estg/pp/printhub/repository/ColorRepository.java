
package pt.utp.estg.pp.printhub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.utp.estg.pp.printhub.entity.Color;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Integer> {

    boolean existsByNameIgnoreCaseAndMaterialId(
            String name, Integer materialId);

    boolean existsByNameIgnoreCaseAndMaterialIdAndIdNot(
            String name, Integer materialId, Integer id);

    // Bloqueia a linha da cor durante uma operação de stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Color c WHERE c.id = :colorId")
    Optional<Color> findByIdForUpdate(@Param("colorId") Integer colorId);
}
