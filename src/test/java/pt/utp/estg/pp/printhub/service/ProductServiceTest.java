
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

    // Criar DTO de produto para reutilizar nos testes
    private ProductDTO criarProdutoDTO(
            String nome,
            String preco,
            Integer categoriaId) {

        return new ProductDTO(
                null,
                nome,
                "Produto de teste",
                new BigDecimal(preco),
                "/images/products/teste.png",
                "10x10x10 cm",
                true,
                categoriaId
        );
    }

    // Criar categoria ativa para os testes
    private Category criarCategoriaAtiva() {
        Category category = new Category();
        category.setId(1);
        category.setName("Decoracao");
        category.setActive(true);
        return category;
    }

    // TESTES DE PESQUISA E CONSULTA

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
        )).thenReturn(new PageImpl<>(
                List.of(product), pageable, 1
        ));

        Page<ProductDTO> result = service.searchProducts(
                "vaso", 2, null, null,
                null, null, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Vaso Espiral", result.getContent().get(0).name());
        assertEquals(
                "/images/products/vaso-espiral.png",
                result.getContent().get(0).imageUrl()
        );

        verify(productRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("Deve devolver uma pagina vazia quando nao existem resultados")
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
        product.setName("Dragao Articulado");
        product.setBasePrice(new BigDecimal("22.90"));
        product.setImageUrl("/images/products/dragao.png");
        product.setCategory(category);

        when(productRepository.findById(3))
                .thenReturn(Optional.of(product));

        ProductDTO result = service.getProductById(3);

        assertEquals(3, result.id());
        assertEquals("Dragao Articulado", result.name());
        assertEquals("/images/products/dragao.png", result.imageUrl());
    }

    @Test
    @DisplayName("Deve lancar excecao quando o produto nao existe")
    void getProductById_NotFound() {
        when(productRepository.findById(999))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.getProductById(999)
        );
    }

    // TESTES DE CRIACAO

    @Test
    @DisplayName("Deve criar um produto com sucesso")
    void createProduct_Success() {
        Category category = criarCategoriaAtiva();

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(10);
                    return product;
                });

        ProductDTO resultado = service.createProduct(
                criarProdutoDTO(" Vaso ", "15.50", 1)
        );

        assertEquals(10, resultado.id());
        assertEquals("Vaso", resultado.name());
        assertEquals(new BigDecimal("15.50"), resultado.basePrice());
        assertTrue(resultado.active());
        assertEquals(1, resultado.categoryId());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Nao deve criar produto sem nome")
    void createProduct_InvalidName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProduct(
                        criarProdutoDTO(" ", "15.50", 1)
                )
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar produto com preco invalido")
    void createProduct_InvalidPrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProduct(
                        criarProdutoDTO("Vaso", "0", 1)
                )
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar produto sem categoria")
    void createProduct_WithoutCategory() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProduct(
                        criarProdutoDTO("Vaso", "15.50", null)
                )
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar produto com categoria inexistente")
    void createProduct_CategoryNotFound() {
        when(categoryRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.createProduct(
                        criarProdutoDTO("Vaso", "15.50", 99)
                )
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nao deve criar produto com categoria inativa")
    void createProduct_InactiveCategory() {
        Category category = criarCategoriaAtiva();
        category.setActive(false);

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createProduct(
                        criarProdutoDTO("Vaso", "15.50", 1)
                )
        );

        verify(productRepository, never()).save(any());
    }

    // TESTES DE ATUALIZACAO

    @Test
    @DisplayName("Deve atualizar um produto")
    void updateProduct_Success() {
        Category category = criarCategoriaAtiva();

        Product product = new Product();
        product.setId(10);
        product.setName("Vaso Antigo");
        product.setBasePrice(new BigDecimal("10.00"));
        product.setCategory(category);
        product.setActive(true);

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        when(categoryRepository.findById(1))
                .thenReturn(Optional.of(category));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductDTO resultado = service.updateProduct(
                10,
                criarProdutoDTO("Vaso Novo", "20.00", 1)
        );

        assertEquals("Vaso Novo", resultado.name());
        assertEquals(
                new BigDecimal("20.00"),
                resultado.basePrice()
        );

        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Nao deve atualizar produto inexistente")
    void updateProduct_NotFound() {
        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.updateProduct(
                        99,
                        criarProdutoDTO("Vaso", "15.50", 1)
                )
        );
    }

    // TESTES DE DESATIVACAO

    @Test
    @DisplayName("Deve desativar um produto")
    void deleteProduct_Success() {
        Product product = new Product();
        product.setId(10);
        product.setActive(true);

        when(productRepository.findById(10))
                .thenReturn(Optional.of(product));

        service.deleteProduct(10);

        assertFalse(product.getActive());

        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("Nao deve desativar produto inexistente")
    void deleteProduct_NotFound() {
        when(productRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.deleteProduct(99)
        );
    }
}
