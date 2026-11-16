package pt.utp.estg.pp.printhub.exception;

/** Palavra-passe atual errada ou nova palavra-passe inválida. Devolve 400. */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}