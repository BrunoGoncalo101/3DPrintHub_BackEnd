package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.NotificationResponse;
import pt.utp.estg.pp.printhub.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificacoes")
@Tag(name = "Notificações", description = "Notificações internas do utilizador autenticado")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Listar as minhas notificações",
            description = "Devolve as notificações do utilizador autenticado, da mais recente para a mais antiga. "
                    + "Permite filtrar apenas pelas não lidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notificações"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada")
    })
    public List<NotificationResponse> listMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "naoLidas", defaultValue = "false") boolean onlyUnread) {
        return notificationService.listMyNotifications(currentUserId(jwt), onlyUnread);
    }

    @GetMapping("/nao-lidas/total")
    @Operation(summary = "Contar notificações não lidas",
            description = "Devolve o número total de notificações por ler do utilizador autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contagem devolvida"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada")
    })
    public Map<String, Long> countUnread(@AuthenticationPrincipal Jwt jwt) {
        long count = notificationService.countUnread(currentUserId(jwt));
        return Map.of("unreadCount", count);
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marcar notificação como lida",
            description = "Marca uma notificação específica do utilizador como lida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação marcada como lida"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada ou não pertence ao utilizador")
    })
    public NotificationResponse markAsRead(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Integer id) {
        return notificationService.markAsRead(currentUserId(jwt), id);
    }

    @PatchMapping("/lidas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marcar todas as notificações como lidas",
            description = "Marca todas as notificações não lidas do utilizador autenticado como lidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Todas as notificações marcadas como lidas"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada")
    })
    public void markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(currentUserId(jwt));
    }

    private Integer currentUserId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "É preciso iniciar sessão.");
        }
        return Integer.valueOf(jwt.getSubject());
    }
}