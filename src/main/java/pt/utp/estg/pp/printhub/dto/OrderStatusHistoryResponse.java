package pt.utp.estg.pp.printhub.dto;

import pt.utp.estg.pp.printhub.model.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        OrderStatus previousStatus,
        OrderStatus newStatus,
        String changedBy,
        LocalDateTime changedAt
) {
}