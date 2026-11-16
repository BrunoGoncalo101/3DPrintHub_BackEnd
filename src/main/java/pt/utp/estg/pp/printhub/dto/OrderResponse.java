package pt.utp.estg.pp.printhub.dto;

import pt.utp.estg.pp.printhub.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Integer id,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        String shippingAddress,
        LocalDateTime createdAt,
        List<OrderLineResponse> lines
) {
}