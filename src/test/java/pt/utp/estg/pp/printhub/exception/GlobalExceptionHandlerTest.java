package pt.utp.estg.pp.printhub.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatus401_usesStandardMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "É preciso iniciar sessão."));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(SecurityMessages.AUTHENTICATION_REQUIRED, 401));
    }

    @Test
    void responseStatus404_keepsSpecificMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Encomenda não encontrada.", 404));
    }

    @Test
    void responseStatusWithoutReason_usesGenericMessageForStatus() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT));

        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Conflito com o estado atual do recurso.", 409));
    }

    @Test
    void accessDenied_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("negado"));

        assertThat(response.getBody()).isEqualTo(new ErrorResponse(SecurityMessages.ACCESS_DENIED, 403));
    }

    @Test
    void authentication_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(
                new InsufficientAuthenticationException("sem sessão"));

        assertThat(response.getBody()).isEqualTo(new ErrorResponse(SecurityMessages.AUTHENTICATION_REQUIRED, 401));
    }
}