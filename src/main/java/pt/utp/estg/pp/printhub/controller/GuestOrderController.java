package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.GuestOrderRequest;
import pt.utp.estg.pp.printhub.dto.GuestPaymentRequest;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.service.OrderService;

@RestController
@RequestMapping("/encomendas/convidado")
@Tag(name = "Encomendas - Convidado", description = "Compra sem conta (RF-025)")
@SecurityRequirements
public class GuestOrderController {

    private final OrderService orderService;

    public GuestOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar encomenda como convidado",
            description = "Sem sessão iniciada. É obrigatório indicar um email ou um telemóvel. "
                    + "Os produtos vêm no pedido (o carrinho do convidado fica no front-end).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encomenda criada"),
            @ApiResponse(responseCode = "400", description = "Sem contacto, sem produtos, quantidade inválida ou cor de outro material"),
            @ApiResponse(responseCode = "404", description = "Produto, material ou cor não encontrados")
    })
    public OrderResponse create(@Valid @RequestBody GuestOrderRequest request) {
        return orderService.createForGuest(request);
    }

    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Pagar encomenda de convidado (simulado)",
            description = "É preciso indicar o email ou o telemóvel usado na encomenda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encomenda paga"),
            @ApiResponse(responseCode = "400", description = "Contacto em falta"),
            @ApiResponse(responseCode = "404", description = "Encomenda não existe ou o contacto não corresponde"),
            @ApiResponse(responseCode = "409", description = "A encomenda já não está por pagar")
    })
    public OrderResponse pay(@PathVariable Integer id,
                             @Valid @RequestBody GuestPaymentRequest request) {
        return orderService.payAsGuest(id, request);
    }
}