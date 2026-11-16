package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestPaymentRequest(
        @NotBlank String contact
) {
}