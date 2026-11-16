package pt.utp.estg.pp.printhub.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.utp.estg.pp.printhub.dto.CreateOrderRequest;
import pt.utp.estg.pp.printhub.dto.OrderLineResponse;
import pt.utp.estg.pp.printhub.dto.OrderResponse;
import pt.utp.estg.pp.printhub.dto.OrderStatusHistoryResponse;
import pt.utp.estg.pp.printhub.dto.UpdateOrderStatusRequest;
import pt.utp.estg.pp.printhub.entity.CartItem;
import pt.utp.estg.pp.printhub.entity.Order;
import pt.utp.estg.pp.printhub.entity.OrderLine;
import pt.utp.estg.pp.printhub.entity.OrderStatusHistory;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.model.OrderStatus;
import pt.utp.estg.pp.printhub.repository.CartItemRepository;
import pt.utp.estg.pp.printhub.repository.OrderRepository;
import pt.utp.estg.pp.printhub.repository.OrderStatusHistoryRepository;
import pt.utp.estg.pp.printhub.repository.UserRepository;
import pt.utp.estg.pp.printhub.dto.AdminOrderSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import pt.utp.estg.pp.printhub.dto.CustomerOrderSummaryResponse;
import pt.utp.estg.pp.printhub.dto.OrderDetailResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import pt.utp.estg.pp.printhub.dto.AddCartItemRequest;
import pt.utp.estg.pp.printhub.dto.GuestOrderRequest;
import pt.utp.estg.pp.printhub.dto.GuestPaymentRequest;
import pt.utp.estg.pp.printhub.entity.Color;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.entity.Product;
import pt.utp.estg.pp.printhub.repository.ColorRepository;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;
import pt.utp.estg.pp.printhub.repository.ProductRepository;

@Service
public class OrderService {

    private static final BigDecimal SHIPPING_COST = new BigDecimal("4.99");
    private static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("50.00");
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository colorRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        UserRepository userRepository,
                        OrderStatusHistoryRepository historyRepository,
                        ProductRepository productRepository,
                        MaterialRepository materialRepository,
                        ColorRepository colorRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.colorRepository = colorRepository;
    }

    // Cria uma encomenda com o que está no carrinho e depois esvazia o carrinho
    @Transactional
    public OrderResponse createFromCart(Integer userId, CreateOrderRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O carrinho está vazio.");
        }

        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setShippingAddress(request.shippingAddress());

        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            if (!Boolean.TRUE.equals(item.getProduct().getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O produto " + item.getProduct().getName() + " já não está disponível.");
            }

            OrderLine line = new OrderLine();
            line.setProduct(item.getProduct());
            line.setMaterial(item.getMaterial());
            line.setColor(item.getColor());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getProduct().getBasePrice());
            order.addLine(line);

            BigDecimal subtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            itemsTotal = itemsTotal.add(subtotal);
        }

        BigDecimal shipping = calculateShipping(itemsTotal);
        order.setShippingCost(shipping);
        order.setTotal(itemsTotal.add(shipping));
        Order saved = orderRepository.save(order);

        // Primeiro registo do histórico: a encomenda nasce "por pagar"
        saveHistory(saved, null, OrderStatus.PENDING_PAYMENT, saved.getUser());

        cartItemRepository.deleteAll(cartItems);

        return toResponse(saved);
    }

    // O admin muda o estado de uma encomenda (só se a transição for permitida)
    @Transactional
    public OrderResponse changeStatus(Integer orderId, Integer adminId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        OrderStatus current = order.getStatus();
        OrderStatus next = request.status();

        if (!current.canChangeTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível passar de " + current + " para " + next + ".");
        }

        order.setStatus(next);
        orderRepository.save(order);

        saveHistory(order, current, next, userRepository.getReferenceById(adminId));

        return toResponse(order);
    }

    // Pagamento simulado: o cliente paga a sua encomenda (passa de PENDING_PAYMENT para PAID)
    @Transactional
    public OrderResponse pay(Integer userId, Integer orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta encomenda já não está por pagar.");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        saveHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, order.getUser());

        return toResponse(order);
    }

    // Lista de encomendas para o admin, com filtros opcionais por estado e por datas
    @Transactional(readOnly = true)
    public List<AdminOrderSummaryResponse> searchForAdmin(OrderStatus status, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data inicial não pode ser depois da data final.");
        }

        LocalDateTime fromTime = null;
        if (from != null) {
            fromTime = from.atStartOfDay();
        }

        LocalDateTime toTime = null;
        if (to != null) {
            toTime = to.plusDays(1).atStartOfDay();
        }

        List<AdminOrderSummaryResponse> result = new ArrayList<>();

        for (Order order : orderRepository.search(status, fromTime, toTime)) {
            String customerName = "Convidado";
            String customerEmail = order.getGuestEmail();
            if (order.getUser() != null) {
                customerName = order.getUser().getName();
                customerEmail = order.getUser().getEmail();
            }

            result.add(new AdminOrderSummaryResponse(
                    order.getId(),
                    customerName,
                    customerEmail,
                    order.getStatus(),
                    order.getTotal(),
                    order.getCreatedAt(),
                    order.getLines().size()
            ));
        }

        return result;
    }

    // Lista as encomendas do cliente, da mais recente para a mais antiga
    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listMine(Integer userId) {
        List<CustomerOrderSummaryResponse> result = new ArrayList<>();

        for (Order order : orderRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            result.add(new CustomerOrderSummaryResponse(
                    order.getId(),
                    order.getStatus(),
                    order.getTotal(),
                    order.getCreatedAt(),
                    order.getLines().size()
            ));
        }

        return result;
    }

    // Detalhe de uma encomenda do cliente: linhas, total e histórico de estados
    @Transactional(readOnly = true)
    public OrderDetailResponse getMine(Integer userId, Integer orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        return new OrderDetailResponse(toResponse(order), getHistory(orderId));
    }

    // Encomenda de convidado: sem conta, com email e/ou telemóvel, e os produtos vêm no pedido
    @Transactional
    public OrderResponse createForGuest(GuestOrderRequest request) {
        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPhone = request.phone() != null && !request.phone().isBlank();

        if (!hasEmail && !hasPhone) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique um email ou um número de telemóvel.");
        }

        Order order = new Order();
        order.setShippingAddress(request.shippingAddress());
        if (hasEmail) {
            order.setGuestEmail(request.email().trim());
        }
        if (hasPhone) {
            order.setGuestPhone(request.phone().trim());
        }

        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (AddCartItemRequest item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .filter(p -> Boolean.TRUE.equals(p.getActive()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));

            Material material = materialRepository.findById(item.materialId())
                    .filter(m -> Boolean.TRUE.equals(m.getActive()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado."));

            Color color = colorRepository.findById(item.colorId())
                    .filter(c -> Boolean.TRUE.equals(c.getAvailable()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cor não encontrada."));

            if (!color.getMaterial().getId().equals(material.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cor não pertence ao material escolhido.");
            }

            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setMaterial(material);
            line.setColor(color);
            line.setQuantity(item.quantity());
            line.setUnitPrice(product.getBasePrice());
            order.addLine(line);

            BigDecimal subtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            itemsTotal = itemsTotal.add(subtotal);
        }

        BigDecimal shipping = calculateShipping(itemsTotal);
        order.setShippingCost(shipping);
        order.setTotal(itemsTotal.add(shipping));
        Order saved = orderRepository.save(order);

        // Sem utilizador: no histórico, "alterado por" fica vazio
        saveHistory(saved, null, OrderStatus.PENDING_PAYMENT, null);

        return toResponse(saved);
    }

    // Pagamento simulado do convidado: tem de indicar o email ou o telemóvel usado na encomenda
    @Transactional
    public OrderResponse payAsGuest(Integer orderId, GuestPaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getUser() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada."));

        String contact = request.contact().trim();
        boolean matches = contact.equalsIgnoreCase(order.getGuestEmail()) || contact.equals(order.getGuestPhone());

        if (!matches) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada.");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta encomenda já não está por pagar.");
        }

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        saveHistory(order, OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, null);

        return toResponse(order);
    }

    // Devolve todas as mudanças de estado de uma encomenda, da mais antiga para a mais recente
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Integer orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Encomenda não encontrada.");
        }

        List<OrderStatusHistoryResponse> result = new ArrayList<>();

        for (OrderStatusHistory h : historyRepository.findByOrderIdOrderByChangedAtAsc(orderId)) {
            String changedBy = null;
            if (h.getChangedBy() != null) {
                changedBy = h.getChangedBy().getName();
            }

            result.add(new OrderStatusHistoryResponse(
                    h.getPreviousStatus(),
                    h.getNewStatus(),
                    changedBy,
                    h.getChangedAt()
            ));
        }

        return result;
    }

    // Grava uma linha no histórico (usado ao criar e ao mudar o estado)
    private void saveHistory(Order order, OrderStatus previous, OrderStatus next, User changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setChangedBy(changedBy);
        historyRepository.save(history);
    }

    // Portes: grátis a partir de 50 €, senão 4,99 €
    private BigDecimal calculateShipping(BigDecimal itemsTotal) {
        if (itemsTotal.compareTo(FREE_SHIPPING_FROM) >= 0) {
            return BigDecimal.ZERO;
        }
        return SHIPPING_COST;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderLineResponse> lines = new ArrayList<>();
        BigDecimal itemsTotal = BigDecimal.ZERO;

        for (OrderLine line : order.getLines()) {
            BigDecimal subtotal = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            itemsTotal = itemsTotal.add(subtotal);
            lines.add(new OrderLineResponse(
                    line.getProduct().getId(),
                    line.getProduct().getName(),
                    line.getMaterial().getName(),
                    line.getColor().getName(),
                    line.getQuantity(),
                    line.getUnitPrice(),
                    subtotal
            ));
        }

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                itemsTotal,
                order.getShippingCost(),
                order.getTotal(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                lines
        );
    }
}