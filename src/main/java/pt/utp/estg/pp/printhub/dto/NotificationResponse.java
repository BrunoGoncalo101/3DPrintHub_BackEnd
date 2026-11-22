package pt.utp.estg.pp.printhub.dto;

import pt.utp.estg.pp.printhub.entity.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Integer id,
        String title,
        String message,
        String type,
        Integer referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getReferenceId(),
                Boolean.TRUE.equals(n.getRead()),
                n.getCreatedAt()
        );
    }
}