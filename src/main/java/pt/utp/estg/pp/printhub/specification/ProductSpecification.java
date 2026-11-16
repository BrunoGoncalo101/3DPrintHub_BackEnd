package pt.utp.estg.pp.printhub.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import pt.utp.estg.pp.printhub.entity.Color;
import pt.utp.estg.pp.printhub.entity.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSpecification {

    public static Specification<Product> withFilters(
            String texto,
            Integer categoriaId,
            Integer materialId,
            Integer corId,
            BigDecimal precoMin,
            BigDecimal precoMax) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            // Pesquisa por nome
            if (texto != null && !texto.isBlank()) {
                String pesquisa = "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pesquisa),
                        cb.like(cb.lower(root.get("description")), pesquisa)
                ));
            }

            if (categoriaId != null) {
                predicates.add(cb.equal(
                        root.get("category").get("id"),
                        categoriaId
                ));
            }

            if (materialId != null || corId != null) {

                Join<Product, Color> colors =
                        root.join("colors", JoinType.INNER);

                if (materialId != null) {
                    predicates.add(cb.equal(
                            colors.get("material").get("id"),
                            materialId
                    ));
                }

                if (corId != null) {
                    predicates.add(cb.equal(
                            colors.get("id"),
                            corId
                    ));
                }

                // Evita produtos duplicados
                query.distinct(true);
            }

            if (precoMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("basePrice"),
                        precoMin
                ));
            }

            if (precoMax != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("basePrice"),
                        precoMax
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
