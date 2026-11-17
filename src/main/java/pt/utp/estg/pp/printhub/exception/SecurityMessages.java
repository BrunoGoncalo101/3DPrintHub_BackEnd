package pt.utp.estg.pp.printhub.exception;

/** Mensagens padrão dos erros de segurança, usadas em toda a API. */
public final class SecurityMessages {

    public static final String AUTHENTICATION_REQUIRED = "É necessário iniciar sessão.";
    public static final String INVALID_SESSION = "Sessão inválida ou expirada. Inicie sessão novamente.";
    public static final String ACCESS_DENIED = "Não tem permissão para aceder a este recurso.";

    private SecurityMessages() {
    }
}