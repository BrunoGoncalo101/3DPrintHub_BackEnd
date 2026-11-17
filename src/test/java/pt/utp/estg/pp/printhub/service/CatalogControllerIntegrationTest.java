package pt.utp.estg.pp.printhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import pt.utp.estg.pp.printhub.config.SecurityConfig;
import pt.utp.estg.pp.printhub.controller.AdminCategoryController;
import pt.utp.estg.pp.printhub.controller.AdminColorController;
import pt.utp.estg.pp.printhub.controller.AdminMaterialController;
import pt.utp.estg.pp.printhub.controller.AdminProductController;
import pt.utp.estg.pp.printhub.controller.CategoryController;
import pt.utp.estg.pp.printhub.controller.MaterialController;
import pt.utp.estg.pp.printhub.controller.ProductController;
import pt.utp.estg.pp.printhub.dto.CategoryDTO;
import pt.utp.estg.pp.printhub.dto.ColorDTO;
import pt.utp.estg.pp.printhub.dto.MaterialDTO;
import pt.utp.estg.pp.printhub.dto.ProductDTO;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;
import pt.utp.estg.pp.printhub.repository.ProductRepository;
import pt.utp.estg.pp.printhub.service.CategoryService;
import pt.utp.estg.pp.printhub.service.ColorService;
import pt.utp.estg.pp.printhub.service.MaterialService;
import pt.utp.estg.pp.printhub.service.ProductService;
import pt.utp.estg.pp.printhub.service.StockService;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogControllerIntegrationTest {

    private MockMvc mockMvc;

    private ProductService productService;
    private CategoryService categoryService;
    private MaterialService materialService;
    private ColorService colorService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        categoryService = mock(CategoryService.class);
        materialService = mock(MaterialService.class);
        colorService = mock(ColorService.class);

        ProductRepository productRepository =
                mock(ProductRepository.class);

        MaterialRepository materialRepository =
                mock(MaterialRepository.class);

        StockService stockService = mock(StockService.class);

        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProductController(
                        productRepository,
                        productService
                ),
                new AdminProductController(productService),
                new CategoryController(categoryService),
                new AdminCategoryController(categoryService),
                new MaterialController(materialRepository),
                new AdminMaterialController(materialService),
                new AdminColorController(
                        colorService,
                        stockService
                )
        ).build();
    }

    private ProductDTO criarProduto() {
        return new ProductDTO(
                1,
                "Vaso",
                "Vaso decorativo",
                new BigDecimal("15.50"),
                "/images/vaso.png",
                "10x10x10 cm",
                true,
                1
        );
    }

    private MaterialDTO criarMaterial() {
        return new MaterialDTO(
                1,
                "PLA",
                new BigDecimal("0.0250"),
                new BigDecimal("1.240"),
                true
        );
    }

    private ColorDTO criarCor() {
        return new ColorDTO(
                1,
                "Vermelho",
                "#FF0000",
                true,
                1
        );
    }

    // PRODUTOS

    @Test
    void deveListarProdutos() throws Exception {
        when(productService.searchProducts(
                any(), any(), any(), any(),
                any(), any(), any()
        )).thenReturn(new PageImpl<>(
                List.of(criarProduto()),
                PageRequest.of(0, 10),
                1
        ));

        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name")
                        .value("Vaso"));
    }

    @Test
    void deveConsultarProduto() throws Exception {
        when(productService.getProductById(1))
                .thenReturn(criarProduto());

        mockMvc.perform(get("/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Vaso"));
    }

    @Test
    void deveDevolver404ProdutoInexistente() throws Exception {
        when(productService.getProductById(99))
                .thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/produtos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRejeitarPaginacaoInvalida() throws Exception {
        mockMvc.perform(get("/produtos")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCriarProduto() throws Exception {
        when(productService.createProduct(any(ProductDTO.class)))
                .thenReturn(criarProduto());

        mockMvc.perform(post("/admin/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarProduto()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vaso"));
    }

    @Test
    void deveAtualizarProduto() throws Exception {
        when(productService.updateProduct(
                eq(1), any(ProductDTO.class)
        )).thenReturn(criarProduto());

        mockMvc.perform(put("/admin/produtos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarProduto()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void deveDesativarProduto() throws Exception {
        mockMvc.perform(delete("/admin/produtos/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(1);
    }

    // CATEGORIAS

    @Test
    void deveListarCategorias() throws Exception {
        when(categoryService.listCategories())
                .thenReturn(List.of(
                        new CategoryDTO(1, "Decoracao", true)
                ));

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Decoracao"));
    }

    @Test
    void deveCriarCategoria() throws Exception {
        when(categoryService.createCategory("Decoracao"))
                .thenReturn(
                        new CategoryDTO(1, "Decoracao", true)
                );

        mockMvc.perform(post("/admin/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Decoracao",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name")
                        .value("Decoracao"));
    }

    @Test
    void deveAtualizarCategoria() throws Exception {
        when(categoryService.updateCategory(
                eq(1), any(CategoryDTO.class)
        )).thenReturn(
                new CategoryDTO(1, "Casa", true)
        );

        mockMvc.perform(put("/admin/categorias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Casa",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Casa"));
    }

    @Test
    void deveDesativarCategoria() throws Exception {
        mockMvc.perform(delete("/admin/categorias/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1);
    }

    // MATERIAIS

    @Test
    void deveListarMateriais() throws Exception {
        mockMvc.perform(get("/materiais"))
                .andExpect(status().isOk());
    }

    @Test
    void deveCriarMaterial() throws Exception {
        when(materialService.createMaterial(any(MaterialDTO.class)))
                .thenReturn(criarMaterial());

        mockMvc.perform(post("/admin/materiais")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarMaterial()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("PLA"));
    }

    @Test
    void deveAtualizarMaterial() throws Exception {
        when(materialService.updateMaterial(
                eq(1), any(MaterialDTO.class)
        )).thenReturn(criarMaterial());

        mockMvc.perform(put("/admin/materiais/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarMaterial()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void deveDesativarMaterial() throws Exception {
        mockMvc.perform(delete("/admin/materiais/1"))
                .andExpect(status().isNoContent());

        verify(materialService).deleteMaterial(1);
    }

    // CORES

    @Test
    void deveCriarCor() throws Exception {
        when(colorService.createColor(any(ColorDTO.class)))
                .thenReturn(criarCor());

        mockMvc.perform(post("/admin/cores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarCor()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hexCode")
                        .value("#FF0000"));
    }

    @Test
    void deveAtualizarCor() throws Exception {
        when(colorService.updateColor(
                eq(1), any(ColorDTO.class)
        )).thenReturn(criarCor());

        mockMvc.perform(put("/admin/cores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                criarCor()
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void deveDesativarCor() throws Exception {
        mockMvc.perform(delete("/admin/cores/1"))
                .andExpect(status().isNoContent());

        verify(colorService).deleteColor(1);
    }
}
