package pt.utp.estg.pp.printhub.dto;

import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomRequestResponse(
        Integer id,
        Integer fileId,
        Integer materialId,
        Integer colorId,
        Integer quantity,
        Integer infillPercent,
        BigDecimal estimatedPrice,
        BigDecimal finalPrice,
        CustomRequestStatus status,
        String rejectionReason,
        Integer userId,
        LocalDateTime createdAt
) {
    public static CustomRequestResponse from(CustomRequest request) {
        return new CustomRequestResponse(
                request.getId(),
                request.getFileId(),
                request.getMaterialId(),
                request.getColorId(),
                request.getQuantity(),
                request.getInfillPercent(),
                request.getEstimatedPrice(),
                request.getFinalPrice(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getUserId(),
                request.getCreatedAt()
        );
    }
}