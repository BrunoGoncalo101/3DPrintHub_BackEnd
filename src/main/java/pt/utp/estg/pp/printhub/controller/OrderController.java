package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.CreateOrderRequest;
import pt.utp.estg.pp.printhub.dto.CustomerOrderSummaryResponse;
import pt.utp.estg.pp.printhub.dto.OrderDetailResponse;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.service.OrderService;

import java.util.List;

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
            description = "Cria uma encomenda com os itens do carrinho, calcula portes e total, e esvazia o carrinho. "
                    + "A encomenda fica no estado PENDING_PAYMENT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encomenda criada"),
            @ApiResponse(responseCode = "400", description = "Carrinho vazio, produto indisponível ou morada inválida"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada")
    })
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.createFromCart(currentUserId(jwt), request);
    }

    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Pagar encomenda (simulado)",
            description = "Marca a encomenda como paga (PENDING_PAYMENT → PAID). Não há cobrança real.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encomenda paga"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "404", description = "Encomenda não existe ou não pertence ao cliente"),
            @ApiResponse(responseCode = "409", description = "A encomenda já não está por pagar")
    })
    public OrderResponse pay(@AuthenticationPrincipal Jwt jwt,
                             @PathVariable Integer id) {
        return orderService.pay(currentUserId(jwt), id);
    }

    @GetMapping
    @Operation(summary = "As minhas encomendas",
            description = "Lista as encomendas do cliente, da mais recente para a mais antiga.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de encomendas (pode vir vazia)"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada")
    })
    public List<CustomerOrderSummaryResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return orderService.listMine(currentUserId(jwt));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da encomenda",
            description = "Linhas, subtotal, portes, total, morada, estado e histórico de estados de uma encomenda do cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalhe da encomenda"),
            @ApiResponse(responseCode = "401", description = "Sem sessão iniciada"),
            @ApiResponse(responseCode = "404", description = "Encomenda não existe ou não pertence ao cliente")
    })
    public OrderDetailResponse getMine(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Integer id) {
        return orderService.getMine(currentUserId(jwt), id);
    }

    private Integer currentUserId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "É preciso iniciar sessão.");
        }
        return Integer.valueOf(jwt.getSubject());
    }
}