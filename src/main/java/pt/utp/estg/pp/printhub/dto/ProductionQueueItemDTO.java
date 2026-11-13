package pt.utp.estg.pp.printhub.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductionQueueItemDTO(
        Integer requestId,
        Integer fileId,
        String fileName,
        Integer materialId,
        Integer colorId,
        Integer quantity,
        Integer infillPercent,
        BigDecimal finalPrice,
        LocalDateTime createdAt
) {}