
package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.utp.estg.pp.printhub.entity.Color;

public interface ColorRepository extends JpaRepository<Color, Integer> {

    boolean existsByNameIgnoreCaseAndMaterialId(String name, Integer materialId);

    boolean existsByNameIgnoreCaseAndMaterialIdAndIdNot(
            String name, Integer materialId, Integer id);
}
