package pt.utp.estg.pp.printhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.NotificationResponse;
import pt.utp.estg.pp.printhub.entity.Notification;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.model.OrderStatus;
import pt.utp.estg.pp.printhub.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Notifica o cliente registado quando o estado de uma encomenda muda.
     * Falhas internas são capturadas para não reverter a atualização da encomenda.
     */
    public void notifyOrderStatusChange(Integer userId, Integer orderId, OrderStatus newStatus) {
        if (userId == null) {
            return; // Encomenda de convidado (sem conta registada)
        }

        try {
            String title = "Atualização da Encomenda #" + orderId;
            String message = "O estado da sua encomenda #" + orderId + " foi atualizado para: " + translateOrderStatus(newStatus) + ".";

            createInternalNotification(userId, title, message, "ORDER_STATUS", orderId);
        } catch (Exception ex) {
            log.error("Falha ao criar notificação para a encomenda #{} do utilizador {}: {}",
                    orderId, userId, ex.getMessage());
        }
    }

    /**
     * Notifica o cliente quando um pedido personalizado é aprovado ou rejeitado.
     */
    public void notifyCustomRequestStatusChange(Integer userId, Integer requestId, CustomRequestStatus newStatus, String extraInfo) {
        if (userId == null) {
            return;
        }

        try {
            String title;
            String message;

            if (newStatus == CustomRequestStatus.APPROVED) {
                title = "Pedido Personalizado #" + requestId + " Aprovado";
                message = "O seu pedido de impressão personalizada #" + requestId + " foi aprovado e entrou na fila de produção."
                        + (extraInfo != null ? " " + extraInfo : "");
            } else if (newStatus == CustomRequestStatus.REJECTED) {
                title = "Pedido Personalizado #" + requestId + " Rejeitado";
                message = "O seu pedido de impressão personalizada #" + requestId + " foi rejeitado."
                        + (extraInfo != null ? " Motivo: " + extraInfo : "");
            } else {
                title = "Atualização do Pedido #" + requestId;
                message = "O estado do seu pedido personalizado #" + requestId + " mudou para: " + newStatus + ".";
            }

            createInternalNotification(userId, title, message, "CUSTOM_REQUEST_STATUS", requestId);
        } catch (Exception ex) {
            log.error("Falha ao criar notificação para o pedido personalizado #{} do utilizador {}: {}",
                    requestId, userId, ex.getMessage());
        }
    }

    private void createInternalNotification(Integer userId, String title, String message, String type, Integer referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("Notificação enviada ao utilizador {} [{} - ref #{}]", userId, type, referenceId);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMyNotifications(Integer userId, boolean onlyUnread) {
        List<Notification> list = onlyUnread
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return list.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada."));

        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private String translateOrderStatus(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "A aguardar pagamento";
            case PAID -> "Paga";
            case PRINTING -> "Em impressão";
            case COMPLETED -> "Concluída";
            case SHIPPED -> "Enviada";
            case CANCELLED -> "Cancelada";
        };
    }
}