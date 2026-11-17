package pt.utp.estg.pp.printhub.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import pt.utp.estg.pp.printhub.dto.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ResponseStatusException;

/** Transforma todas as exceções no formato JSON de erro comum da API. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "O ficheiro excede o limite máximo de 50 MB.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "O pedido tem de ser do tipo multipart/form-data.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Falta o ficheiro no pedido (campo '" + ex.getRequestPartName() + "').");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Falta o parâmetro '" + ex.getParameterName() + "'.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Valor inválido no parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade na base de dados", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "Dados inválidos (verifique se o utilizador existe).");
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        log.error("Falha ao gravar o ficheiro", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    /** Erros lançados com ResponseStatusException: mantém a mensagem, exceto nos 401/403, que usam as padrão. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String message = switch (status.value()) {
            case 401 -> SecurityMessages.AUTHENTICATION_REQUIRED;
            case 403 -> SecurityMessages.ACCESS_DENIED;
            default -> ex.getReason() != null ? ex.getReason() : getMessageForStatusCode(status.value());
        };
        return buildResponse(status, message);
    }

    /** Falta de autenticação detetada dentro de um controller ou service. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, SecurityMessages.AUTHENTICATION_REQUIRED);
    }

    /** Falta de permissão detetada dentro de um controller ou service (por exemplo, @PreAuthorize). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, SecurityMessages.ACCESS_DENIED);
    }

    /** Trata erros genéricos e exceções HTTP do Spring. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return buildResponse(status, getMessageForStatusCode(status.value()));
        }
        log.error("Erro inesperado", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor.");
    }

    private static String getMessageForStatusCode(int code) {
        return switch (code) {
            case 400 -> "Pedido inválido.";
            case 401 -> SecurityMessages.AUTHENTICATION_REQUIRED;
            case 403 -> SecurityMessages.ACCESS_DENIED;
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método não permitido.";
            case 409 -> "Conflito com o estado atual do recurso.";
            case 415 -> "Tipo de conteúdo não suportado.";
            default -> "Erro no pedido.";
        };
    }

    private static ResponseEntity<ErrorResponse> buildResponse(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, status.value()));
    }
}