
package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pt.utp.estg.pp.printhub.entity.Product;

public interface ProductRepository extends
        JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {
}
