package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.CreateOrderRequest;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.service.OrderService;

@RestController
@RequestMapping("/encomendas")
@Tag(name = "Encomendas", description = "Encomendas do cliente com sessão iniciada")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar encomenda",
            description = "Cria uma encomenda com os itens do carrinho e esvazia o carrinho.")
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createFromCart(currentUserId(jwt), request);
    }

    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Pagar encomenda (simulado)",
            description = "Marca a encomenda como paga. Só funciona se estiver por pagar.")
    public OrderResponse pay(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable Integer id) {
        return orderService.pay(currentUserId(jwt), id);
    }

    private Integer currentUserId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "É preciso iniciar sessão.");
        }
        return Integer.valueOf(jwt.getSubject());
    }
}