package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.NotificationResponse;
import pt.utp.estg.pp.printhub.entity.Notification;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.model.OrderStatus;
import pt.utp.estg.pp.printhub.repository.NotificationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyOrderStatusChange_ShouldSaveNotification_WhenUserIdIsPresent() {
        notificationService.notifyOrderStatusChange(10, 42, OrderStatus.PRINTING);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10, saved.getUserId());
        assertEquals(42, saved.getReferenceId());
        assertEquals("ORDER_STATUS", saved.getType());
        assertTrue(saved.getMessage().contains("Em impressão"));
        assertFalse(saved.getRead());
    }

    @Test
    void notifyOrderStatusChange_ShouldDoNothing_WhenGuestOrder() {
        notificationService.notifyOrderStatusChange(null, 42, OrderStatus.PAID);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyOrderStatusChange_ShouldNotThrowException_WhenRepositoryFails() {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException("Erro na BD"));

        assertDoesNotThrow(() ->
                notificationService.notifyOrderStatusChange(10, 42, OrderStatus.SHIPPED)
        );
    }

    @Test
    void notifyCustomRequestStatusChange_ShouldSaveApprovedNotification() {
        notificationService.notifyCustomRequestStatusChange(
                7, 99, CustomRequestStatus.APPROVED, "Preço final: 15.50 €."
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(7, saved.getUserId());
        assertEquals(99, saved.getReferenceId());
        assertEquals("CUSTOM_REQUEST_STATUS", saved.getType());
        assertTrue(saved.getTitle().contains("Aprovado"));
        assertTrue(saved.getMessage().contains("15.50"));
    }

    @Test
    void markAsRead_ShouldUpdateReadFlag_WhenNotificationBelongsToUser() {
        Notification notification = new Notification();
        notification.setId(5);
        notification.setUserId(10);
        notification.setTitle("Teste");
        notification.setMessage("Mensagem");
        notification.setType("ORDER_STATUS");
        notification.setRead(false);

        when(notificationRepository.findByIdAndUserId(5, 10)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(10, 5);

        assertTrue(response.read());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_ShouldThrowNotFound_WhenNotificationDoesNotExist() {
        when(notificationRepository.findByIdAndUserId(999, 10)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(10, 999));
    }
}