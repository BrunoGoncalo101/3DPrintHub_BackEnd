package pt.utp.estg.pp.printhub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private final SecurityErrorHandler handler = new SecurityErrorHandler();

    @Test
    void expiredOrInvalidToken_returns401WithSessionMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new InvalidBearerTokenException("Jwt expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"Sessão inválida ou expirada. Inicie sessão novamente.\",\"code\":401}");
    }

    @Test
    void missingToken_returns401AskingToLogIn() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"É necessário iniciar sessão.\",\"code\":401}");
    }

    @Test
    void accessDenied_returns403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("Access denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .isEqualTo("{\"error\":\"Não tem permissão para aceder a este recurso.\",\"code\":403}");
    }
}