package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GuestOrderRequest(
        @NotBlank @Size(max = 255) String shippingAddress,
        @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @NotEmpty @Size(max = 20) List<@Valid AddCartItemRequest> items
) {
}