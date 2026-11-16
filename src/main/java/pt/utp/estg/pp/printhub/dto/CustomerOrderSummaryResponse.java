package pt.utp.estg.pp.printhub.dto;

import pt.utp.estg.pp.printhub.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerOrderSummaryResponse(
        Integer id,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        int itemCount
) {
}