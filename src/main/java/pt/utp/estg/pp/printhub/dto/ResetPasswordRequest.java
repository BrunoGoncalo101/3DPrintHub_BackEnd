package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redefinição da palavra-passe com o token recebido por email. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}