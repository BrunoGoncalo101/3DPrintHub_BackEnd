package pt.utp.estg.pp.printhub.controller;

import io.swagger.v3.oas.annotations.Operation;
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
public class GuestOrderController {

    private final OrderService orderService;

    public GuestOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar encomenda como convidado",
            description = "Sem sessão iniciada. É obrigatório indicar um email ou um telemóvel.")
    public OrderResponse create(@Valid @RequestBody GuestOrderRequest request) {
        return orderService.createForGuest(request);
    }

    @PostMapping("/{id}/pagamento")
    @Operation(summary = "Pagar encomenda de convidado (simulado)",
            description = "É preciso indicar o email ou o telemóvel usado na encomenda.")
    public OrderResponse pay(@PathVariable Integer id,
                             @Valid @RequestBody GuestPaymentRequest request) {
        return orderService.payAsGuest(id, request);
    }
}