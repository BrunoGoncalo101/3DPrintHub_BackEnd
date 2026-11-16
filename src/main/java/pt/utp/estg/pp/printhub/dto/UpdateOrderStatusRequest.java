package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.NotNull;
import pt.utp.estg.pp.printhub.model.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {
}