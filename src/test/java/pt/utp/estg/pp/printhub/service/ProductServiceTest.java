
package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import pt.utp.estg.pp.printhub.dto.ProductDTO;
import pt.utp.estg.pp.printhub.entity.Category;
import pt.utp.estg.pp.printhub.entity.Product;
import pt.utp.estg.pp.printhub.repository.CategoryRepository;
import pt.utp.estg.pp.printhub.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService service;

    @Test
    @DisplayName("Deve pesquisar produtos e devolver resultados paginados")
    void searchProducts_Success() {
        Category category = new Category();
        category.setId(2);

        Product product = new Product();
        product.setId(1);
        product.setName("Vaso Espiral");
        product.setBasePrice(new BigDecimal("16.50"));
        product.setImageUrl("/images/products/vaso-espiral.png");
        product.setActive(true);
        product.setCategory(category);

        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        Page<ProductDTO> result = service.searchProducts(
                "vaso", 2, null, null,
                null, null, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Vaso Espiral", result.getContent().get(0).name());
        assertEquals("/images/products/vaso-espiral.png",
                result.getContent().get(0).imageUrl());

        verify(productRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("Deve devolver uma página vazia quando não existem resultados")
    void searchProducts_Empty() {
        Pageable pageable = PageRequest.of(0, 5);

        when(productRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        Page<ProductDTO> result = service.searchProducts(
                "inexistente", null, null, null,
                null, null, pageable
        );

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve devolver um produto pelo ID com a respetiva imagem")
    void getProductById_Success() {
        Category category = new Category();
        category.setId(1);

        Product product = new Product();
        product.setId(3);
        product.setName("Dragão Articulado");
        product.setBasePrice(new BigDecimal("22.90"));
        product.setImageUrl("/images/products/dragao.png");
        product.setCategory(category);

        when(productRepository.findById(3))
                .thenReturn(Optional.of(product));

        ProductDTO result = service.getProductById(3);

        assertEquals(3, result.id());
        assertEquals("Dragão Articulado", result.name());
        assertEquals("/images/products/dragao.png", result.imageUrl());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o produto não existe")
    void getProductById_NotFound() {
        when(productRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.getProductById(999));
    }
}
