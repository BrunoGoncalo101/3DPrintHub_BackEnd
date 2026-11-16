package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.dto.OrderStatusHistoryResponse;
import pt.utp.estg.pp.printhub.dto.UpdateOrderStatusRequest;
import pt.utp.estg.pp.printhub.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/admin/encomendas")
@Tag(name = "Admin - Encomendas", description = "Gestão de encomendas (só administradores)")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Mudar estado da encomenda",
            description = "Muda o estado se a transição for permitida e regista no histórico.")
    public OrderResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Integer id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        Integer adminId = Integer.valueOf(jwt.getSubject());
        return orderService.changeStatus(id, adminId, request);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Ver histórico da encomenda",
            description = "Lista todas as mudanças de estado, da mais antiga para a mais recente.")
    public List<OrderStatusHistoryResponse> getHistory(@PathVariable Integer id) {
        return orderService.getHistory(id);
    }
}