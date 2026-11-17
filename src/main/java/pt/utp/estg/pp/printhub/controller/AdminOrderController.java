package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.AdminOrderSummaryResponse;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.dto.OrderStatusHistoryResponse;
import pt.utp.estg.pp.printhub.dto.UpdateOrderStatusRequest;
import pt.utp.estg.pp.printhub.model.OrderStatus;
import pt.utp.estg.pp.printhub.service.OrderService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/encomendas")
@Tag(name = "Admin - Encomendas", description = "Gestão de encomendas (só administradores)")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Listar encomendas",
            description = "Lista todas as encomendas, da mais recente para a mais antiga. "
                    + "Filtros opcionais: estado e intervalo de datas (formato AAAA-MM-DD). "
                    + "Encomendas de convidado aparecem com o nome \"Convidado\".")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de encomendas (pode vir vazia)"),
            @ApiResponse(responseCode = "400", description = "Estado inválido ou data inicial depois da final"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "403", description = "O utilizador não é administrador")
    })
    public List<AdminOrderSummaryResponse> list(
            @RequestParam(required = false) OrderStatus estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return orderService.searchForAdmin(estado, de, ate);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Mudar estado da encomenda",
            description = "Transições permitidas: PENDING_PAYMENT → PAID ou CANCELLED; PAID → PRINTING ou CANCELLED; "
                    + "PRINTING → COMPLETED; COMPLETED → SHIPPED. Cada mudança fica no histórico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado alterado"),
            @ApiResponse(responseCode = "400", description = "Estado em falta ou inválido"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "403", description = "O utilizador não é administrador"),
            @ApiResponse(responseCode = "404", description = "Encomenda não encontrada"),
            @ApiResponse(responseCode = "409", description = "Transição de estado não permitida")
    })
    public OrderResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Integer id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        Integer adminId = Integer.valueOf(jwt.getSubject());
        return orderService.changeStatus(id, adminId, request);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Ver histórico da encomenda",
            description = "Lista todas as mudanças de estado, da mais antiga para a mais recente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico da encomenda"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "403", description = "O utilizador não é administrador"),
            @ApiResponse(responseCode = "404", description = "Encomenda não encontrada")
    })
    public List<OrderStatusHistoryResponse> getHistory(@PathVariable Integer id) {
        return orderService.getHistory(id);
    }
}