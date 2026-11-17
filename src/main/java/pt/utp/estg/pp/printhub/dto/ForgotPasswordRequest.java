package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Pedido de recuperação da palavra-passe. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}