package pt.utp.estg.pp.printhub.dto;

import java.util.List;

public record OrderDetailResponse(
        OrderResponse order,
        List<OrderStatusHistoryResponse> history
) {
}