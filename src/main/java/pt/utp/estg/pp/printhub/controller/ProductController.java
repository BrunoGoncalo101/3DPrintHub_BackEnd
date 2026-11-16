
package pt.utp.estg.pp.printhub.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.utp.estg.pp.printhub.dto.ProductDTO;
import pt.utp.estg.pp.printhub.repository.ProductRepository;
import pt.utp.estg.pp.printhub.service.ProductService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/produtos")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public ProductController(
            ProductRepository productRepository,
            ProductService productService) {

        this.productRepository = productRepository;
        this.productService = productService;
    }

    // Listar e pesquisar produtos com filtros e paginação
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> listProducts(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer materialId,
            @RequestParam(required = false) Integer corId,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().build();
        }

        if ((precoMin != null && precoMin.signum() < 0)
                || (precoMax != null && precoMax.signum() < 0)
                || (precoMin != null && precoMax != null
                && precoMin.compareTo(precoMax) > 0)) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<ProductDTO> products = productService.searchProducts(
                texto,
                categoriaId,
                materialId,
                corId,
                precoMin,
                precoMax,
                pageable
        );

        return ResponseEntity.ok(products);
    }


    // Consultar um produto pelo ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Integer id) {

        try {
            ProductDTO product = productService.getProductById(id);
            return ResponseEntity.ok(product);

        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
