
package pt.utp.estg.pp.printhub.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.utp.estg.pp.printhub.config.SecurityConfig;
import pt.utp.estg.pp.printhub.controller.AdminProductController;
import pt.utp.estg.pp.printhub.dto.ProductDTO;
import pt.utp.estg.pp.printhub.exception.SecurityErrorHandler;
import pt.utp.estg.pp.printhub.service.ProductService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@EnableWebSecurity
@Import({
        SecurityConfig.class,
        SecurityErrorHandler.class
})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.jwt.secret=chave-exclusiva-para-testes-com-mais-de-32-caracteres"
})
class CatalogSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String PRODUTO_JSON = """
        {
            "name": "Vaso",
            "description": "Vaso decorativo",
            "basePrice": 15.50,
            "imageUrl": "/images/vaso.png",
            "dimensions": "10x10x10 cm",
            "active": true,
            "categoryId": 1
        }
        """;

    // TESTE 1: Sem autenticação, não pode criar produtos.
    @Test
    void deveBloquearCriacaoProdutoSemAutenticacao() throws Exception {

        mockMvc.perform(post("/admin/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUTO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // TESTE 2: Um cliente não pode criar produtos.
    @Test
    void deveBloquearCriacaoProdutoParaCliente() throws Exception {

        mockMvc.perform(post("/admin/produtos")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUTO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // TESTE 3: Um administrador pode criar produtos.
    @Test
    void devePermitirCriacaoProdutoParaAdministrador() throws Exception {

        ProductDTO produto = new ProductDTO(
                1,
                "Vaso",
                "Vaso decorativo",
                new BigDecimal("15.50"),
                "/images/vaso.png",
                "10x10x10 cm",
                true,
                1
        );

        when(productService.createProduct(any(ProductDTO.class)))
                .thenReturn(produto);

        mockMvc.perform(post("/admin/produtos")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUTO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Vaso"));
    }

    // TESTE 4: Um cliente não pode eliminar produtos.
    @Test
    void deveBloquearEliminacaoProdutoParaCliente() throws Exception {

        mockMvc.perform(delete("/admin/produtos/1")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_CUSTOMER")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // TESTE 5: Um administrador pode eliminar produtos.
    @Test
    void devePermitirEliminacaoProdutoParaAdministrador() throws Exception {

        mockMvc.perform(delete("/admin/produtos/1")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                        )))
                .andExpect(status().isNoContent());
    }
}
